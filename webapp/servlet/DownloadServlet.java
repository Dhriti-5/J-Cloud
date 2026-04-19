package servlet;

import dao.ChunkDAO;
import dao.ChunkLocationDAO;
import dao.FileDAO;
import dao.NodeDAO;
import shared.Chunk;
import shared.ChunkLocation;
import shared.DataNodeClient;
import shared.FileMetadata;
import shared.NodeInfo;
import shared.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Day 8 — Download Servlet (The Stitcher)
 *
 * Flow:
 *   1. User clicks download link: /download?file_id=5
 *   2. We look up the file metadata (name, size) from PostgreSQL
 *   3. We fetch all chunks ordered by chunk_index from PostgreSQL
 *   4. For each chunk, we query chunk_locations to find which node holds it
 *   5. We open a TCP socket to that DataNode, send GET_CHUNK command
 *   6. We stream the returned bytes directly into the HTTP response
 *   7. Browser receives a seamless download of the reassembled file
 *
 * URL: /download?file_id=<id>
 */
@WebServlet("/download")
public class DownloadServlet extends HttpServlet {

    private FileDAO fileDAO;
    private ChunkDAO chunkDAO;
    private ChunkLocationDAO chunkLocationDAO;
    private NodeDAO nodeDAO;

    @Override
    public void init() throws ServletException {
        fileDAO          = new FileDAO();
        chunkDAO         = new ChunkDAO();
        chunkLocationDAO = new ChunkLocationDAO();
        nodeDAO          = new NodeDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── 1. Auth check ────────────────────────────────────────────────────
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login");
            return;
        }
        User user = (User) session.getAttribute("user");

        // ── 2. Parse file_id param ────────────────────────────────────────────
        String fileIdParam = request.getParameter("file_id");
        if (fileIdParam == null || fileIdParam.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file_id parameter");
            return;
        }

        int fileId;
        try {
            fileId = Integer.parseInt(fileIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file_id");
            return;
        }

        // ── 3. Fetch file metadata from PostgreSQL ───────────────────────────
        FileMetadata file = fileDAO.getFileById(fileId);
        if (file == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        // Security: only owner can download their own files
        if (file.getOwnerId() != user.getUserId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        // Check if at least one node is active
        List<NodeInfo> activeNodes = nodeDAO.getAllActiveNodes();
        if (activeNodes == null || activeNodes.isEmpty()) {
            request.setAttribute("error",
                "Cannot download file: All data nodes are currently offline. Please try again later.");
            request.getRequestDispatcher("/files.jsp").forward(request, response);
            return;
        }

        // ── 4. Fetch all chunks ordered by chunk_index ───────────────────────
        List<Chunk> chunks = chunkDAO.getChunksByFileId(fileId);
        if (chunks == null || chunks.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No chunks found for this file");
            return;
        }

        System.out.println("⬇ Download request: fileId=" + fileId
                + " | file=" + file.getFileName()
                + " | chunks=" + chunks.size()
                + " | user=" + user.getUsername());

        // ── 5. Set HTTP headers to force browser download ────────────────────
        String encodedName = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8.name())
                .replace("+", "%20");   // spaces as %20, not +

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + file.getFileName() + "\"; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(file.getFileSize());
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        // ── 6. Stream chunks sequentially into HTTP response ─────────────────
        OutputStream out = response.getOutputStream();

        for (Chunk chunk : chunks) {
            // Find which node holds this chunk
            List<ChunkLocation> locations = chunkLocationDAO.getChunkLocations(chunk.getChunkId());

            if (locations == null || locations.isEmpty()) {
                System.err.println("✗ No location found for chunkId=" + chunk.getChunkId());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Chunk location missing for chunk " + chunk.getChunkId());
                return;
            }

            // Try each replica location (fault tolerance: first available node wins)
            byte[] chunkData = null;
            for (ChunkLocation location : locations) {
                NodeInfo node = nodeDAO.getNodeById(location.getNodeId());
                if (node == null || !"ACTIVE".equalsIgnoreCase(node.getStatus())) {
                    System.out.println("⚠ Node " + location.getNodeId() + " is not ACTIVE, trying next...");
                    continue;
                }

                System.out.println("⬇ Fetching chunk " + chunk.getChunkIndex()
                        + " (id=" + chunk.getChunkId() + ") from "
                        + node.getNodeName() + " [" + node.getIpAddress() + ":" + node.getPort() + "]");

                DataNodeClient client = new DataNodeClient(node.getIpAddress(), node.getPort());
                chunkData = client.getChunk(chunk.getChunkId());

                if (chunkData != null) {
                    break; // Got it, move on
                }
                System.out.println("⚠ Failed to fetch from " + node.getNodeName() + ", trying next replica...");
            }

            if (chunkData == null) {
                System.err.println("✗ All replicas failed for chunkId=" + chunk.getChunkId());
                // Cannot recover — the client will get a broken/partial download
                // We flush what we have and stop
                out.flush();
                return;
            }

            // Write chunk bytes directly to HTTP output stream
            out.write(chunkData);
            System.out.println("✓ Streamed chunk " + chunk.getChunkIndex()
                    + " (" + chunkData.length + " bytes)");
        }

        out.flush();
        System.out.println("✓ Download complete: " + file.getFileName()
                + " (" + file.getFileSize() + " bytes)");
    }
}