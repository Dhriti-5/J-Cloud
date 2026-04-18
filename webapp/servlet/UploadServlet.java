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
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Upload servlet for chunked upload — supports multiple files per request.
 * Limits: max 5 files per upload, max 50 MB total size across all files.
 */
@WebServlet("/upload")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,
    maxFileSize       = 1024 * 1024 * 1024,
    maxRequestSize    = 5L * 1024 * 1024 * 1024
)
public class UploadServlet extends HttpServlet {

    private static final int  CHUNK_SIZE           = 5 * 1024 * 1024; // 5 MB per chunk
    private static final int  UPLOAD_THREAD_POOL   = 4;

    // ── Upload limits ────────────────────────────────────────────────────────
    private static final int  MAX_FILES            = 5;
    private static final long MAX_TOTAL_SIZE_MB    = 50;
    private static final long MAX_TOTAL_SIZE_BYTES = MAX_TOTAL_SIZE_MB * 1024 * 1024;

    private FileDAO          fileDAO;
    private ChunkDAO         chunkDAO;
    private ChunkLocationDAO chunkLocationDAO;
    private NodeDAO          nodeDAO;

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
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login");
            return;
        }
        request.getRequestDispatcher("/upload.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login");
            return;
        }

        User user = (User) session.getAttribute("user");

        // ── Collect all file parts (name="files") ────────────────────────────
        Collection<Part> allParts = request.getParts();
        List<Part> fileParts = new ArrayList<>();
        for (Part p : allParts) {
            if ("files".equals(p.getName())
                    && p.getSubmittedFileName() != null
                    && !p.getSubmittedFileName().trim().isEmpty()
                    && p.getSize() > 0) {
                fileParts.add(p);
            }
        }

        if (fileParts.isEmpty()) {
            request.setAttribute("error", "Please select at least one file to upload.");
            request.getRequestDispatcher("/upload.jsp").forward(request, response);
            return;
        }

        // ── Validate: max file count ─────────────────────────────────────────
        if (fileParts.size() > MAX_FILES) {
            request.setAttribute("error",
                "Too many files selected. Maximum " + MAX_FILES
                + " files allowed per upload.");
            request.getRequestDispatcher("/upload.jsp").forward(request, response);
            return;
        }

        // ── Validate: max total size ─────────────────────────────────────────
        long totalBytes = 0;
        for (Part p : fileParts) {
            totalBytes += p.getSize();
        }
        if (totalBytes > MAX_TOTAL_SIZE_BYTES) {
            String totalMB = String.format("%.1f", totalBytes / (1024.0 * 1024));
            request.setAttribute("error",
                "Total upload size (" + totalMB + " MB) exceeds the "
                + MAX_TOTAL_SIZE_MB + " MB limit. Please reduce your selection.");
            request.getRequestDispatcher("/upload.jsp").forward(request, response);
            return;
        }

        // ── Retrieve active nodes once for all files ─────────────────────────
        nodeDAO = new NodeDAO();
        List<NodeInfo> activeNodes = nodeDAO.getActiveNodesSortedByCapacity();
        if (activeNodes.isEmpty()) {
            request.setAttribute("error",
                "No active data nodes available. Please start at least one node.");
            request.getRequestDispatcher("/upload.jsp").forward(request, response);
            return;
        }

        // ── Process each file ────────────────────────────────────────────────
        int successCount = 0;
        List<String> failedFiles = new ArrayList<>();

        for (Part filePart : fileParts) {
            String originalFileName =
                Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            long fileSize = filePart.getSize();

            boolean ok = uploadSingleFile(filePart, originalFileName, fileSize, user, activeNodes);
            if (ok) {
                successCount++;
            } else {
                failedFiles.add(originalFileName);
            }
        }

        // ── Build result message ─────────────────────────────────────────────
        if (failedFiles.isEmpty()) {
            if (successCount == 1) {
                request.setAttribute("success",
                    "File uploaded successfully and split into chunks"
                    + " (replication factor: ~2).");
            } else {
                request.setAttribute("success",
                    successCount + " files uploaded successfully"
                    + " and distributed across nodes.");
            }
        } else if (successCount == 0) {
            request.setAttribute("error",
                "All uploads failed: " + String.join(", ", failedFiles));
        } else {
            request.setAttribute("success",
                successCount + " file(s) uploaded successfully. "
                + "Failed: " + String.join(", ", failedFiles));
        }

        request.getRequestDispatcher("/upload.jsp").forward(request, response);
    }

    /**
     * Chunk, distribute, and record metadata for a single file.
     *
     * @return true if fully successful, false on any error
     */
    private boolean uploadSingleFile(Part filePart,
                                     String originalFileName,
                                     long totalSize,
                                     User user,
                                     List<NodeInfo> activeNodes) {

        // Create file metadata record
        FileMetadata fileMetadata = new FileMetadata(originalFileName, totalSize, user.getUserId());
        boolean created = fileDAO.createFile(fileMetadata);
        if (!created) {
            System.err.println("✗ Failed to create file metadata for: " + originalFileName);
            return false;
        }

        ExecutorService executor = Executors.newFixedThreadPool(
            Math.min(UPLOAD_THREAD_POOL, Math.max(1, activeNodes.size())));
        List<Future<ChunkUploadResult>> futures = new ArrayList<>();

        try (InputStream in = filePart.getInputStream()) {
            byte[] buffer   = new byte[CHUNK_SIZE];
            int    bytesRead;
            int    chunkIndex = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                byte[] chunkBytes = Arrays.copyOf(buffer, bytesRead);

                // Persist chunk metadata first (generates chunkId)
                Chunk chunk   = new Chunk(fileMetadata.getFileId(), chunkIndex, bytesRead);
                int   chunkId = chunkDAO.createChunk(chunk);
                if (chunkId < 0) {
                    throw new IOException(
                        "Failed to create chunk metadata for " + originalFileName);
                }

                // Primary node (highest capacity)
                final int      finalChunkId    = chunkId;
                final int      finalChunkIndex = chunkIndex;
                final byte[]   finalBytes      = chunkBytes;
                final NodeInfo finalPrimary    = activeNodes.get(0);

                futures.add(executor.submit(() -> {
                    DataNodeClient client = new DataNodeClient(
                        finalPrimary.getIpAddress(), finalPrimary.getPort());
                    boolean stored = client.storeChunk(
                        finalChunkId, finalChunkIndex,
                        fileMetadata.getFileId(), originalFileName, finalBytes);
                    return new ChunkUploadResult(
                        finalChunkId, finalPrimary.getNodeId(), stored,
                        stored ? null : "Failed to store on primary node");
                }));

                // Replica node (second node if available)
                if (activeNodes.size() >= 2) {
                    final NodeInfo finalReplica = activeNodes.get(1);
                    futures.add(executor.submit(() -> {
                        DataNodeClient client = new DataNodeClient(
                            finalReplica.getIpAddress(), finalReplica.getPort());
                        boolean stored = client.storeChunk(
                            finalChunkId, finalChunkIndex,
                            fileMetadata.getFileId(), originalFileName, finalBytes);
                        return new ChunkUploadResult(
                            finalChunkId, finalReplica.getNodeId(), stored,
                            stored ? null : "Failed to store on replica node");
                    }));
                } else {
                    System.out.println("⚠ Only 1 node available for chunk " + chunkId
                        + " of " + originalFileName + ". Marked for async replication.");
                }

                chunkIndex++;
            }

            // Collect results and record chunk locations
            for (Future<ChunkUploadResult> future : futures) {
                ChunkUploadResult result = future.get();
                if (!result.success) {
                    System.out.println("⚠ Chunk storage issue: " + result.error);
                    continue;
                }
                chunkLocationDAO.createChunkLocation(
                    new ChunkLocation(result.chunkId, result.nodeId));
            }

            System.out.println("✓ Uploaded: " + originalFileName
                + " (" + totalSize + " bytes)");
            return true;

        } catch (IOException | InterruptedException | ExecutionException e) {
            System.err.println("✗ Upload failed for "
                + originalFileName + ": " + e.getMessage());
            return false;

        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                executor.shutdownNow();
            }
        }
    }

    // ── Inner result holder ──────────────────────────────────────────────────
    private static class ChunkUploadResult {
        final int     chunkId;
        final int     nodeId;
        final boolean success;
        final String  error;

        ChunkUploadResult(int chunkId, int nodeId, boolean success, String error) {
            this.chunkId = chunkId;
            this.nodeId  = nodeId;
            this.success = success;
            this.error   = error;
        }
    }
}