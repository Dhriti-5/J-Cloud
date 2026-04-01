package master;

import shared.NodeInfo;
import dao.NodeDAO;

/**
 * Recovery Manager - Day 12
 * Handles automatic recovery of dead nodes
 * 
 * This manager is triggered by HeartbeatMonitor when a node timeout is detected.
 * It orchestrates replication, re-balancing, and failover procedures.
 */
public class RecoveryManager {

    private static NodeDAO nodeDAO = new NodeDAO();

    /**
     * Initiate recovery for a dead node
     * 
     * Day 12 Tasks:
     * 1. Re-replicate chunks from dead node to healthy nodes
     * 2. Update chunk_locations to point to new nodes
     * 3. Mark node as INACTIVE (not DEAD)
     * 4. Notify admin dashboard
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
                return;
            }

            System.out.println("   📍 Node ID: " + deadNode.getNodeId());
            System.out.println("   📊 Capacity: " + deadNode.getStorageCapacity() + " bytes");

            // Step 2: Find all chunks on this dead node
            System.out.println("   🔍 Scanning for chunks on dead node...");
            // TODO: Query chunk_locations for this node_id
            // TODO: For each chunk, find healthy replica nodes
            // TODO: If no replicas exist, re-replicate from any available node

            // Step 3: Re-replicate chunks
            System.out.println("   📤 Re-replicating chunks to healthy nodes...");
            // TODO: Call ReplicationManager to place chunks on healthy nodes
            // TODO: Update chunk_locations table with new node assignments

            // Step 4: Update node status
            System.out.println("   🔄 Updating node status in database...");
            nodeDAO.updateNodeStatusByName(nodeName, "INACTIVE");

            System.out.println("✓ Recovery initiated for: " + nodeName);
            System.out.println("  Further recovery steps (Day 12) pending...\n");

        } catch (Exception e) {
            System.err.println("✗ Error in recovery manager for node: " + nodeName);
            e.printStackTrace();
        }
    }

    /**
     * Check health of all nodes and trigger recovery if needed
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
                    // Recovery is already triggered by HeartbeatMonitor
                }
            }

            System.out.println("   ✓ Health check complete (" + deadCount + " dead nodes)");

        } catch (Exception e) {
            System.err.println("✗ Error in cluster health check");
            e.printStackTrace();
        }
    }
}
