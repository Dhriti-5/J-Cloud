package master;

import utils.Config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * Master Node Server - The Brain of J-Cloud
 * 
 * Features:
 * - Thread pool for scalable client handling (50 concurrent connections)
 * - Accepts node registrations and heartbeats
 * - Monitors node health automatically
 * - Non-blocking architecture prevents server stalls
 */
public class MasterServer {

    private static final int PORT = 9000;
    private static final int THREAD_POOL_SIZE = 50;
    private static final int BACKGROUND_TASK_POOL_SIZE = 100;
    
    // Thread-safe map to track last heartbeat from each node
    // Key: nodeName, Value: timestamp in milliseconds
    public static final ConcurrentHashMap<String, Long> lastHeartbeatMap = new ConcurrentHashMap<>();
    
    // Thread-safe map to store node IDs
    // Key: nodeName, Value: nodeId
    public static final ConcurrentHashMap<String, Integer> nodeIdMap = new ConcurrentHashMap<>();
    private static final ExecutorService backgroundTaskPool = Executors.newFixedThreadPool(BACKGROUND_TASK_POOL_SIZE);
    
    private ExecutorService clientHandlerPool;
    private ScheduledExecutorService heartbeatMonitorService;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public static void submitBackgroundTask(Runnable task) {
        backgroundTaskPool.execute(task);
    }

    public MasterServer() {
        // Fixed thread pool prevents memory exhaustion under heavy load
        this.clientHandlerPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        // Scheduled service for periodic heartbeat monitoring
        this.heartbeatMonitorService = Executors.newScheduledThreadPool(1);
    }

    /**
     * Start the Master Node server
     */
    public void start() {
        try {
            InetAddress bindAddr = InetAddress.getByName(Config.MASTER_HOST);
            serverSocket = new ServerSocket(PORT, 50, bindAddr);

            System.out.println("╔════════════════════════════════════════════╗");
            System.out.println("║   J-CLOUD MASTER NODE STARTED              ║");
            System.out.println("║   Port: " + PORT + "                              ║");
            System.out.println("║   Thread Pool Size: " + THREAD_POOL_SIZE + "                   ║");
            System.out.println("╚════════════════════════════════════════════╝\n");

            // Start the heartbeat death monitor
            startHeartbeatMonitor();

            // Start the replication manager (Day 10 - autonomous healing thread)
            startReplicationManager();

            // Accept incoming connections and delegate to thread pool
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("→ New connection from: " + clientSocket.getInetAddress().getHostAddress());
                    
                    // Submit to thread pool instead of creating new thread
                    clientHandlerPool.execute(new ClientHandler(clientSocket));
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("✗ Error accepting client connection");
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("✗ Failed to start Master Server on port " + PORT);
            e.printStackTrace();
        }
    }

    /**
     * Start the heartbeat monitor
     * Runs every 10 seconds, marks nodes as DEAD if no heartbeat for 15 seconds
     */
    private void startHeartbeatMonitor() {
        HeartbeatMonitor monitor = new HeartbeatMonitor();
        
        // Schedule with fixed delay: wait 10 seconds, then run every 10 seconds
        heartbeatMonitorService.scheduleWithFixedDelay(
            monitor,
            10,  // Initial delay
            10,  // Period
            TimeUnit.SECONDS
        );
        
        System.out.println("✓ Heartbeat monitor started (check interval: 10s, timeout: 15s)\n");
    }

    /**
     * Day 10 — Start the replication manager
     * 
     * Runs as a background thread that periodically scans for under-replicated chunks
     * and autonomously heals them by copying to healthy nodes (self-healing).
     */
    private void startReplicationManager() {
        ReplicationManager replManager = new ReplicationManager();
        
        // Submit to background task pool (separate from client handlers)
        submitBackgroundTask(replManager);
        
        System.out.println("✓ Replication manager started (autonomous healing thread)\n");
    }

    /**
     * Graceful shutdown
     */
    public void shutdown() {
        System.out.println("\n⚠ Shutting down Master Server...");
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Shutdown thread pools gracefully
        clientHandlerPool.shutdown();
        heartbeatMonitorService.shutdown();
        backgroundTaskPool.shutdown();
        
        try {
            if (!clientHandlerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                clientHandlerPool.shutdownNow();
            }
            if (!heartbeatMonitorService.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatMonitorService.shutdownNow();
            }
            if (!backgroundTaskPool.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundTaskPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientHandlerPool.shutdownNow();
            heartbeatMonitorService.shutdownNow();
            backgroundTaskPool.shutdownNow();
        }

        System.out.println("✓ Master Server stopped");
    }

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        MasterServer server = new MasterServer();
        
        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
        }));
        
        server.start();
    }
}
