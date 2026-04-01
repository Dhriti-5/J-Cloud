package master;

import shared.NodeInfo;
import dao.NodeDAO;
import dao.EventLogDAO;
import dao.ChunkLocationDAO;

/**
 * Recovery Manager - Day 12
 * Orchestrates automatic recovery of dead nodes
 * 
 * This manager is triggered by HeartbeatMonitor when a node timeout is detected.
 * It coordinates with ReplicationManager (Day 10) for chunk healing.
 * 
 * Key Design: 
 * - HeartbeatMonitor purges chunk_locations (metadata cleanup)
 * - ReplicationManager automatically detects under-replicated chunks
 * - RecoveryManager logs the recovery process for visibility
 */
public class RecoveryManager {

    private static NodeDAO nodeDAO = new NodeDAO();
    private static EventLogDAO eventLogDAO = new EventLogDAO();
    private static ChunkLocationDAO chunkLocationDAO = new ChunkLocationDAO();

    /**
     * Initiate recovery for a dead node
     * 
     * Day 12 Tasks:
     * 1. ✓ METADATA PURGE (done by HeartbeatMonitor)
     * 2. ✓ EVENT LOGGING (done by HeartbeatMonitor)
     * 3. Monitor chunk re-replication (handled by Day 10 ReplicationManager)
     * 
     * This method serves as the orchestration point and future extension point
     * for more sophisticated recovery strategies (rebalancing, priority scheduling, etc.)
     * 
     * @param nodeName the name of the dead node
     */
    public static void recoverNode(String nodeName) {
        try {
            System.out.println("\n🔧 [Recovery Manager] Initiating recovery for node: " + nodeName);

            // Step 1: Fetch the dead node info
            NodeInfo deadNode = nodeDAO.getNodeByName(nodeName);
            if (deadNode == null) {
                System.err.println("✗ Recovery: Node not found in database: " + nodeName);
                eventLogDAO.logEvent("RECOVERY_FAILED", 
                                   "Node " + nodeName + " not found in database");
                return;
            }

            System.out.println("   📍 Node ID: " + deadNode.getNodeId());
            System.out.println("   📊 Lost Capacity: " + deadNode.getStorageCapacity() + " bytes");

            // Step 2: Get current system health
            java.util.Map<String, Integer> health = chunkLocationDAO.getSystemHealth();
            System.out.println("   🏥 System health BEFORE recovery:");
            System.out.println("      - Healthy chunks: " + health.get("healthy_chunks"));
            System.out.println("      - Under-replicated: " + health.get("under_replicated_chunks"));

            eventLogDAO.logEvent("RECOVERY_START", 
                               "Node " + nodeName + " (" + deadNode.getNodeId() + ") recovery initiated. " +
                               "Under-replicated chunks: " + health.get("under_replicated_chunks"));

            // Step 3: The actual healing happens in ReplicationManager (Day 10)
            System.out.println("   ⏳ Waiting for Day 10 ReplicationManager to heal chunks...");
            System.out.println("   (ReplicationManager runs every 60 seconds and will fix chunks within deadline)");

            System.out.println("✓ Recovery process started for: " + nodeName);
            System.out.println("  Next steps triggered by ReplicationManager (60s cycle)\n");

        } catch (Exception e) {
            System.err.println("✗ Error in recovery manager for node: " + nodeName);
            e.printStackTrace();
            eventLogDAO.logEvent("RECOVERY_ERROR", 
                               "Error recovering node " + nodeName + ": " + e.getMessage());
        }
    }

    /**
     * Log successful chunk recovery
     * Called by ReplicationManager after successfully replicating a chunk
     * 
     * @param chunkId the chunk that was recovered
     * @param fromNodeId source node
     * @param toNodeId target node
     */
    public static void logChunkRecovery(int chunkId, int fromNodeId, int toNodeId) {
        try {
            String msg = "Chunk " + chunkId + " recovered from node " + fromNodeId + 
                        " to node " + toNodeId;
            eventLogDAO.logEvent("RECOVERY_SUCCESS", msg);
            System.out.println("  ✓ " + msg);
        } catch (Exception e) {
            System.err.println("✗ Error logging chunk recovery");
            e.printStackTrace();
        }
    }

    /**
     * Check health of all nodes and log recovery progress if any nodes are down
     * Can be used as a background task for comprehensive cluster monitoring
     */
    public static void checkAndRecoverAll() {
        System.out.println("\n🏥 [Recovery Manager] Running cluster health check...");

        try {
            // Fetch all nodes
            java.util.List<NodeInfo> allNodes = nodeDAO.getAllNodes();

            if (allNodes == null || allNodes.isEmpty()) {
                System.out.println("   ℹ No nodes to check");
                return;
            }

            int deadCount = 0;
            for (NodeInfo node : allNodes) {
                if ("DEAD".equals(node.getStatus())) {
                    deadCount++;
                    System.out.println("   💀 Dead node detected: " + node.getNodeName());
                }
            }

            if (deadCount == 0) {
                System.out.println("   ✓ All nodes healthy");
            } else {
                System.out.println("   ⚠ " + deadCount + " dead node(s) in recovery");
                
                // Check system health
                java.util.Map<String, Integer> health = chunkLocationDAO.getSystemHealth();
                System.out.println("   📊 System Health:");
                System.out.println("      - Healthy chunks: " + health.get("healthy_chunks"));
                System.out.println("      - Under-replicated: " + health.get("under_replicated_chunks"));
            }

        } catch (Exception e) {
            System.err.println("✗ Error in cluster health check");
            e.printStackTrace();
        }
    }
}
