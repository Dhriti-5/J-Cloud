package master;

import dao.NodeDAO;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Heartbeat Monitor - Death Detector (Day 11)
 * Runs as a scheduled background task every 10 seconds
 * Marks nodes as DEAD if no heartbeat received in 15 seconds
 * 
 * Scalable Approach: In-Memory Tracking + DB Sync
 * - Maintains lastHeartbeatMap (in-memory)
 * - Syncs status changes to PostgreSQL
 * - Triggers RecoveryManager on node failure
 */
public class HeartbeatMonitor implements Runnable {

    private static final long HEARTBEAT_TIMEOUT_MS = 15000; // 15 seconds (Day 11 spec)
    private NodeDAO nodeDAO;
    private static Set<String> deadNodesTracked = new HashSet<>(); // Prevent duplicate recovery triggers

    public HeartbeatMonitor() {
        this.nodeDAO = new NodeDAO();
    }

    @Override
    public void run() {
        try {
            long currentTime = System.currentTimeMillis();
            
            System.out.println("⏰ Running heartbeat health check...");

            // Check all nodes in the heartbeat map (in-memory tracking)
            for (Map.Entry<String, Long> entry : MasterServer.lastHeartbeatMap.entrySet()) {
                String nodeName = entry.getKey();
                long lastHeartbeat = entry.getValue();
                long timeSinceLastHeartbeat = currentTime - lastHeartbeat;
                Integer nodeId = MasterServer.nodeIdMap.get(nodeName);

                // Check if node has timed out (> 15 seconds without heartbeat)
                if (timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT_MS) {
                    // Node is DEAD
                    System.out.println("☠ Node DEAD: " + nodeName + 
                                       " (last seen " + (timeSinceLastHeartbeat / 1000) + "s ago)");
                    
                    // Update PostgreSQL database with DEAD status
                    if (nodeId != null) {
                        nodeDAO.updateNodeStatus(nodeId, "DEAD");
                        System.out.println("  ✓ Updated database: Node " + nodeName + " marked as DEAD");
                    }
                    
                    // Trigger recovery ONLY once per node death
                    if (!deadNodesTracked.contains(nodeName)) {
                        deadNodesTracked.add(nodeName);
                        System.out.println("  🔧 Triggering recovery process...");
                        RecoveryManager.recoverNode(nodeName); // DAY 12 RECOVERY TRIGGER
                    }
                    
                } else {
                    // Node is ALIVE
                    System.out.println("✓ Node ALIVE: " + nodeName + 
                                       " (last seen " + (timeSinceLastHeartbeat / 1000) + "s ago)");
                    
                    // If node was previously dead and is now responding, clean up tracking
                    if (deadNodesTracked.contains(nodeName)) {
                        deadNodesTracked.remove(nodeName);
                        System.out.println("  ✓ Node recovered: " + nodeName);
                        if (nodeId != null) {
                            nodeDAO.updateNodeStatus(nodeId, "ACTIVE");
                        }
                    }
                }
            }

            if (MasterServer.lastHeartbeatMap.isEmpty()) {
                System.out.println("⚠ No nodes registered yet");
            }

            System.out.println("  📊 Status: " + MasterServer.lastHeartbeatMap.size() + 
                             " nodes monitored, " + deadNodesTracked.size() + " dead\n");

        } catch (Exception e) {
            System.err.println("✗ Error in heartbeat monitor");
            e.printStackTrace();
        }
    }
}
