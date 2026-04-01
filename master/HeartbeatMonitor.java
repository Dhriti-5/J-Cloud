package master;

import dao.NodeDAO;
import dao.EventLogDAO;
import dao.ChunkLocationDAO;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Heartbeat Monitor - Death Detector (Day 11)
 * Enhanced for Day 12: Metadata Purge + Event Logging
 * 
 * Runs as a scheduled background task every 10 seconds
 * Marks nodes as DEAD if no heartbeat received in 15 seconds
 * 
 * Scalable Approach: In-Memory Tracking + DB Sync + Metadata Cleanup
 * - Maintains lastHeartbeatMap (in-memory)
 * - Syncs status changes to PostgreSQL (nodes table)
 * - PURGES chunk_locations for dead nodes (CRITICAL: wakes up Day 10 Healer!)
 * - Logs all events for administrative visibility
 * - Triggers RecoveryManager on node failure
 */
public class HeartbeatMonitor implements Runnable {

    private static final long HEARTBEAT_TIMEOUT_MS = 15000; // 15 seconds (Day 11 spec)
    private NodeDAO nodeDAO;
    private EventLogDAO eventLogDAO;  // Day 12: Event logging
    private ChunkLocationDAO chunkLocationDAO;  // Day 12: Metadata purge
    private static Set<String> deadNodesTracked = new HashSet<>(); // Prevent duplicate recovery triggers

    public HeartbeatMonitor() {
        this.nodeDAO = new NodeDAO();
        this.eventLogDAO = new EventLogDAO();
        this.chunkLocationDAO = new ChunkLocationDAO();
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
                        
                        System.out.println("  🔧 [Day 12] Initiating metadata purge & recovery...");
                        
                        // **TASK 1: METADATA PURGE (Day 12) - The Critical Trigger**
                        // Delete chunk_locations for this dead node
                        // This makes chunks appear under-replicated in the database
                        // ReplicationManager will find them within 60 seconds and heal them
                        if (nodeId != null) {
                            int deletedMappings = chunkLocationDAO.deleteChunkLocationsByNodeId(nodeId);
                            String purgeMsg = "Metadata purged: " + deletedMappings + " chunk_locations deleted";
                            System.out.println("  " + purgeMsg);
                            
                            // **TASK 2: EVENT LOGGING (Day 12) - Log the purge**
                            eventLogDAO.logEvent("METADATA_PURGE", 
                                               "Node " + nodeName + " (" + nodeId + ") failed. " + purgeMsg);
                        }
                        
                        // Log the failure event
                        eventLogDAO.logEvent("NODE_FAILURE", 
                                           "Node " + nodeName + " failed to respond (timeout > 15s). Recovery initiated.");
                        
                        // Call recovery manager for Day 12 recovery steps
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
                            // Log recovery event
                            eventLogDAO.logEvent("NODE_RECOVERY", 
                                               "Node " + nodeName + " is back online and responding to heartbeats.");
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
