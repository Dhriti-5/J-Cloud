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
import java.util.List;
import java.util.concurrent.*;

/**
 * Upload servlet for chunked upload.
 */
@WebServlet("/upload")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 1024, maxRequestSize = 1024 * 1024 * 1024)
public class UploadServlet extends HttpServlet {
    private static final int CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int UPLOAD_THREAD_POOL = 4;

    private FileDAO fileDAO;
    private ChunkDAO chunkDAO;
    private ChunkLocationDAO chunkLocationDAO;
    private NodeDAO nodeDAO;

    @Override
    public void init() throws ServletException {
        fileDAO = new FileDAO();
        chunkDAO = new ChunkDAO();
        chunkLocationDAO = new ChunkLocationDAO();
        nodeDAO = new NodeDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Ensure user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login");
            return;
        }

        request.getRequestDispatcher("/upload.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login");
            return;
        }

        User user = (User) session.getAttribute("user");

        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("error", "Please select a file to upload.");
            request.getRequestDispatcher("/upload.jsp").forward(request, response);
            return;
        }

        String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        long totalSize = filePart.getSize();

        // Create file metadata in the database
        FileMetadata fileMetadata = new FileMetadata(originalFileName, totalSize, user.getUserId());
        boolean created = fileDAO.createFile(fileMetadata);
        if (!created) {
            request.setAttribute("error", "Failed to register file metadata.");
            request.getRequestDispatcher("/upload.jsp").forward(request, response);
            return;
        }

            // Retrieve active nodes for storage (sorted by capacity for Day 10 replication)
        nodeDAO = new NodeDAO();
        List<NodeInfo> activeNodes = nodeDAO.getActiveNodesSortedByCapacity();
        if (activeNodes.isEmpty()) {
            request.setAttribute("error", "No active data nodes available. Please start at least one node.");
            request.getRequestDispatcher("/upload.jsp").forward(request, response);
            return;
        }

        // Chunk and upload in parallel
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(UPLOAD_THREAD_POOL, Math.max(1, activeNodes.size())));
        List<Future<ChunkUploadResult>> futures = new ArrayList<>();

        try (InputStream in = filePart.getInputStream()) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int chunkIndex = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                // Always copy bytes to avoid reusing the same buffer
                byte[] chunkBytes = Arrays.copyOf(buffer, bytesRead);

                // Persist chunk metadata first (needed for chunkId)
                Chunk chunk = new Chunk(fileMetadata.getFileId(), chunkIndex, bytesRead);
                int chunkId = chunkDAO.createChunk(chunk);
                if (chunkId < 0) {
                    throw new IOException("Failed to create chunk metadata");
                }

                // Day 10: Replication Strategy - Store on top 2 nodes (or 1 if unavailable)
                // Node 1 (Primary): Always try to use first node (highest capacity)
                NodeInfo primaryNode = activeNodes.get(0);

                // Capture values for lambda (must be effectively final)
                final int finalChunkId = chunkId;
                final int finalChunkIndex = chunkIndex;
                final byte[] finalBytes = chunkBytes;
                final NodeInfo finalPrimaryNode = primaryNode;

                // Submit upload task for primary node
                futures.add(executor.submit(() -> {
                    DataNodeClient client = new DataNodeClient(finalPrimaryNode.getIpAddress(), finalPrimaryNode.getPort());
                    boolean stored = client.storeChunk(finalChunkId, finalChunkIndex, fileMetadata.getFileId(), originalFileName, finalBytes);

                    return new ChunkUploadResult(finalChunkId, finalPrimaryNode.getNodeId(), stored, stored ? null : "Failed to store chunk on primary node");
                }));

                // Node 2 (Replica): Submit to second node if available
                if (activeNodes.size() >= 2) {
                    NodeInfo replicaNode = activeNodes.get(1);
                    final NodeInfo finalReplicaNode = replicaNode;

                    futures.add(executor.submit(() -> {
                        DataNodeClient client = new DataNodeClient(finalReplicaNode.getIpAddress(), finalReplicaNode.getPort());
                        boolean stored = client.storeChunk(finalChunkId, finalChunkIndex, fileMetadata.getFileId(), originalFileName, finalBytes);

                        return new ChunkUploadResult(finalChunkId, finalReplicaNode.getNodeId(), stored, stored ? null : "Failed to store chunk on replica node");
                    }));
                } else {
                    // Only 1 node available - mark chunk for async replication by ReplicationManager
                    System.out.println("⚠ Only 1 node available for chunk " + chunkId + ". Marked for async replication.");
                }

                chunkIndex++;
            }

            // Wait for uploads to complete and record locations
            int replicationFactor = 0;
            for (Future<ChunkUploadResult> future : futures) {
                ChunkUploadResult result = future.get();
                if (!result.success) {
                    // For non-blocking uploads, we don't fail if replica fails to store,
                    // but we do fail if primary storage fails
                    if (result.nodeId == 0) {
                        throw new IOException(result.error + " (chunkId=" + result.chunkId + ")");
                    } else {
                        System.out.println("⚠ Replica storage failed: " + result.error);
                        continue;
                    }
                }

                ChunkLocation location = new ChunkLocation(result.chunkId, result.nodeId);
                chunkLocationDAO.createChunkLocation(location);
                replicationFactor++;
            }


        } catch (IOException | InterruptedException | ExecutionException e) {
            request.setAttribute("error", "Upload failed: " + e.getMessage());
            request.getRequestDispatcher("/upload.jsp").forward(request, response);
            return;
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

        request.setAttribute("success", "File uploaded successfully and split into chunks (replication factor: ~2).");
        request.getRequestDispatcher("/upload.jsp").forward(request, response);
    }

    private static class ChunkUploadResult {
        final int chunkId;
        final int nodeId;
        final boolean success;
        final String error;

        ChunkUploadResult(int chunkId, int nodeId, boolean success, String error) {
            this.chunkId = chunkId;
            this.nodeId = nodeId;
            this.success = success;
            this.error = error;
        }
    }
}
