package master;

import dao.NodeDAO;

import java.util.Map;

/**
 * Heartbeat Monitor - Death Detector
 * Runs as a scheduled background task every 10 seconds
 * Marks nodes as DEAD if no heartbeat received in 15 seconds
 */
public class HeartbeatMonitor implements Runnable {

    private static final long HEARTBEAT_TIMEOUT_MS = 15000; // 15 seconds
    private NodeDAO nodeDAO;

    public HeartbeatMonitor() {
        this.nodeDAO = new NodeDAO();
    }

    @Override
    public void run() {
        try {
            long currentTime = System.currentTimeMillis();
            
            System.out.println("⏰ Running heartbeat health check...");

            // Check all nodes in the heartbeat map
            for (Map.Entry<String, Long> entry : MasterServer.lastHeartbeatMap.entrySet()) {
                String nodeName = entry.getKey();
                long lastHeartbeat = entry.getValue();
                long timeSinceLastHeartbeat = currentTime - lastHeartbeat;

                // Check if node has timed out
                if (timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT_MS) {
                    System.out.println("☠ Node DEAD: " + nodeName + 
                                       " (last seen " + (timeSinceLastHeartbeat / 1000) + "s ago)");
                    
                    // Mark as DEAD in database
                    Integer nodeId = MasterServer.nodeIdMap.get(nodeName);
                    if (nodeId != null) {
                        nodeDAO.updateNodeStatus(nodeId, "DEAD");
                    }
                    
                    // Optionally remove from heartbeat map
                    // MasterServer.lastHeartbeatMap.remove(nodeName);
                } else {
                    System.out.println("✓ Node ALIVE: " + nodeName + 
                                       " (last seen " + (timeSinceLastHeartbeat / 1000) + "s ago)");
                }
            }

            if (MasterServer.lastHeartbeatMap.isEmpty()) {
                System.out.println("⚠ No nodes registered yet");
            }

            System.out.println("  " + MasterServer.lastHeartbeatMap.size() + " nodes monitored\n");

        } catch (Exception e) {
            System.err.println("✗ Error in heartbeat monitor");
            e.printStackTrace();
        }
    }
}
