package master;

import dao.ChunkDAO;
import dao.ChunkLocationDAO;
import dao.NodeDAO;
import shared.Chunk;
import shared.ChunkLocation;
import shared.DataNodeClient;
import shared.NodeInfo;

import java.util.List;
import java.util.Map;

/**
 * ReplicationManager - The "Healer" Thread
 * 
 * Day 10 Implementation: Autonomous Replication Recovery
 * 
 * This background thread periodically scans the database for under-replicated chunks
 * (chunks stored on fewer than 2 nodes) and autonomously repairs them by:
 * 
 * 1. Finding chunks with fewer than 2 replicas
 * 2. Identifying a source node with the chunk
 * 3. Selecting a healthy target node that doesn't have the chunk yet
 * 4. Copying the chunk from source to target
 * 5. Updating the chunk_locations table
 * 
 * This enables:
 * - Non-blocking uploads (users can upload even if only 1 node is available)
 * - Automatic fault recovery (if a node crashes, the Replication Manager heals the system)
 * - Scalable network efficiency (healing happens asynchronously during low-traffic periods)
 */
public class ReplicationManager implements Runnable {

    private static final long CHECK_INTERVAL_MS = 60 * 1000; // Run every 60 seconds
    private static final int MAX_REPLICATE_PER_RUN = 5; // Limit replication tasks per cycle to avoid overload
    
    private final ChunkLocationDAO chunkLocationDAO;
    private final ChunkDAO chunkDAO;
    private final NodeDAO nodeDAO;
    private volatile boolean running = true;

    public ReplicationManager() {
        this.chunkLocationDAO = new ChunkLocationDAO();
        this.chunkDAO = new ChunkDAO();
        this.nodeDAO = new NodeDAO();
    }

    @Override
    public void run() {
        System.out.println("✓ ReplicationManager thread started (check interval: " + (CHECK_INTERVAL_MS / 1000) + "s)");

        while (running) {
            try {
                // Sleep for the check interval
                Thread.sleep(CHECK_INTERVAL_MS);

                // Perform replication healing
                healUnderReplicatedChunks();

            } catch (InterruptedException e) {
                if (running) {
                    System.err.println("⚠ ReplicationManager interrupted");
                }
                break;
            } catch (Exception e) {
                System.err.println("✗ Error in ReplicationManager.run()");
                e.printStackTrace();
            }
        }

        System.out.println("✓ ReplicationManager thread stopped");
    }

    /**
     * Find under-replicated chunks and autonomously heal them.
     * 
     * Algorithm:
     * 1. Query DB for chunks with < 2 replicas (LIMIT 5 to avoid overwhelming)
     * 2. For each chunk:
     *    a. Find the node where it currently lives (source)
     *    b. Find an ALIVE node that doesn't have it yet (target)
     *    c. If both found: GET_CHUNK from source, STORE_CHUNK to target
     *    d. Update chunk_locations table
     */
    private void healUnderReplicatedChunks() {
        List<Map<String, Integer>> underReplicatedChunks = chunkLocationDAO.getUnderReplicatedChunks();

        if (underReplicatedChunks.isEmpty()) {
            // No work to do this cycle
            return;
        }

        System.out.println("\n↻ ReplicationManager: Found " + underReplicatedChunks.size() + " under-replicated chunk(s), healing (max " + MAX_REPLICATE_PER_RUN + ")...");

        int healedCount = 0;
        for (Map<String, Integer> chunkInfo : underReplicatedChunks) {
            if (healedCount >= MAX_REPLICATE_PER_RUN) {
                System.out.println("↻ ReplicationManager: Reached max replication limit (" + MAX_REPLICATE_PER_RUN + "), deferring remaining chunks to next cycle");
                break;
            }

            int chunkId = chunkInfo.get("chunk_id");
            int fileId = chunkInfo.get("file_id");

            try {
                if (replicateChunk(chunkId, fileId)) {
                    healedCount++;
                }
            } catch (Exception e) {
                System.err.println("✗ Failed to heal chunk " + chunkId + ": " + e.getMessage());
            }
        }

        if (healedCount > 0) {
            System.out.println("✓ ReplicationManager: Successfully healed " + healedCount + " chunk(s)");
        }
    }

    /**
     * Replicate a single under-replicated chunk.
     * 
     * @param chunkId the chunk to replicate
     * @param fileId the file containing the chunk (for context)
     * @return true if replication successful, false otherwise
     */
    private boolean replicateChunk(int chunkId, int fileId) {
        // Get the current replica locations
        List<ChunkLocation> locations = chunkLocationDAO.getChunkLocations(chunkId);

        if (locations.isEmpty()) {
            System.err.println("✗ ReplicationManager: Chunk " + chunkId + " has no replicas (data loss!)");
            return false;
        }

        // The source node is the one that has the chunk
        int sourceNodeId = locations.get(0).getNodeId();
        NodeInfo sourceNode = nodeDAO.getNodeById(sourceNodeId);

        if (sourceNode == null) {
            System.err.println("✗ ReplicationManager: Source node " + sourceNodeId + " not found");
            return false;
        }

        // Get all active nodes
        List<NodeInfo> activeNodes = nodeDAO.getActiveNodesSortedByCapacity();

        // Collect the node IDs that already have this chunk
        java.util.Set<Integer> nodesWithChunk = new java.util.HashSet<>();
        for (ChunkLocation loc : locations) {
            nodesWithChunk.add(loc.getNodeId());
        }

        // Find a target node that doesn't have the chunk yet
        NodeInfo targetNode = null;
        for (NodeInfo node : activeNodes) {
            if (!nodesWithChunk.contains(node.getNodeId())) {
                targetNode = node;
                break;
            }
        }

        if (targetNode == null) {
            System.out.println("⚠ ReplicationManager: No suitable target node found for chunk " + chunkId);
            return false;
        }

        // Fetch chunk metadata (contains replica count info for logging)
        Chunk chunk = chunkDAO.getChunkById(chunkId);
        if (chunk == null) {
            System.err.println("✗ ReplicationManager: Chunk metadata not found for chunkId=" + chunkId);
            return false;
        }

        // Step 1: Fetch the chunk bytes from source node
        System.out.println("  → Fetching chunk " + chunkId + " from source node " + sourceNode.getNodeName() 
                         + " (" + sourceNode.getIpAddress() + ":" + sourceNode.getPort() + ")");

        DataNodeClient sourceClient = new DataNodeClient(sourceNode.getIpAddress(), sourceNode.getPort());
        byte[] chunkBytes = sourceClient.getChunk(chunkId);

        if (chunkBytes == null || chunkBytes.length == 0) {
            System.err.println("✗ ReplicationManager: Failed to fetch chunk " + chunkId + " from source node");
            return false;
        }

        // Step 2: Store the chunk on target node
        System.out.println("  → Storing chunk " + chunkId + " on target node " + targetNode.getNodeName() 
                         + " (" + targetNode.getIpAddress() + ":" + targetNode.getPort() + ")");

        DataNodeClient targetClient = new DataNodeClient(targetNode.getIpAddress(), targetNode.getPort());
        boolean stored = targetClient.storeChunk(
            chunkId,
            chunk.getChunkIndex(),
            fileId,
            "replicated_chunk_" + chunkId,
            chunkBytes
        );

        if (!stored) {
            System.err.println("✗ ReplicationManager: Failed to store chunk " + chunkId + " on target node");
            return false;
        }

        // Step 3: Update chunk_locations table
        ChunkLocation newLocation = new ChunkLocation(chunkId, targetNode.getNodeId());
        boolean locationUpdated = chunkLocationDAO.createChunkLocation(newLocation);

        if (!locationUpdated) {
            System.err.println("✗ ReplicationManager: Failed to update chunk_locations for chunkId=" + chunkId);
            return false;
        }

        // Success!
        System.out.println("✓ ReplicationManager: Successfully replicated chunk " + chunkId 
                         + " from " + sourceNode.getNodeName() + " to " + targetNode.getNodeName());

        return true;
    }

    /**
     * Gracefully stop the replication manager thread.
     */
    public void stop() {
        running = false;
    }
}
