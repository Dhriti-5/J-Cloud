package datanode;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * Data Node Server - The Worker
 * 
 * Features:
 * - Auto-registers with Master Node on startup
 * - Sends heartbeat every 5 seconds to prove it's alive
 * - Uses ScheduledExecutorService for reliable heartbeat timing
 * - Graceful shutdown with resource cleanup
 */
public class DataNodeServer {

    // Configuration - modify these for different nodes
    private final String nodeName;
    private final String ipAddress;
    private final int port;
    private final long storageCapacity;
    
    // Master Node connection details
    private static final String MASTER_HOST = "localhost";
    private static final int MASTER_PORT = 9000;
    
    // Heartbeat configuration
    private static final int HEARTBEAT_INTERVAL_SECONDS = 5;
    
    private ScheduledExecutorService heartbeatService;
    private volatile boolean running = true;

    /**
     * Constructor for Data Node
     */
    public DataNodeServer(String nodeName, String ipAddress, int port, long storageCapacity) {
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        this.port = port;
        this.storageCapacity = storageCapacity;
        this.heartbeatService = Executors.newScheduledThreadPool(1);
    }

    /**
     * Start the Data Node
     */
    public void start() {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   J-CLOUD DATA NODE STARTING               ║");
        System.out.println("║   Node: " + String.format("%-35s", nodeName) + "║");
        System.out.println("║   Port: " + String.format("%-35d", port) + "║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        // Step 1: Register with Master Node
        if (!registerWithMaster()) {
            System.err.println("✗ Failed to register with Master. Exiting...");
            return;
        }

        // Step 2: Start periodic heartbeat
        startHeartbeat();

        System.out.println("\n✓ Data Node is now running and sending heartbeats");
        System.out.println("  Press Ctrl+C to stop\n");

        // Keep the main thread alive
        try {
            while (running) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Register this node with the Master Node
     * Protocol: REGISTER_NODE|nodeName|ipAddress|port|capacity
     */
    private boolean registerWithMaster() {
        System.out.println("→ Registering with Master Node at " + MASTER_HOST + ":" + MASTER_PORT);
        
        try (
            Socket socket = new Socket(MASTER_HOST, MASTER_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            // Send registration message
            String registrationMessage = String.format("REGISTER_NODE|%s|%s|%d|%d",
                nodeName, ipAddress, port, storageCapacity);
            
            out.println(registrationMessage);
            System.out.println("  Sent: " + registrationMessage);

            // Wait for response
            String response = in.readLine();
            System.out.println("  Response: " + response);

            if (response != null && response.startsWith("OK")) {
                System.out.println("✓ Registration successful!\n");
                return true;
            } else {
                System.err.println("✗ Registration failed: " + response);
                return false;
            }

        } catch (IOException e) {
            System.err.println("✗ Cannot connect to Master Node");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Start the heartbeat service
     * Sends HEARTBEAT message every 5 seconds
     */
    private void startHeartbeat() {
        System.out.println("♥ Starting heartbeat service (interval: " + HEARTBEAT_INTERVAL_SECONDS + "s)");
        
        // Schedule heartbeat task with fixed rate
        heartbeatService.scheduleAtFixedRate(
            this::sendHeartbeat,
            HEARTBEAT_INTERVAL_SECONDS,  // Initial delay
            HEARTBEAT_INTERVAL_SECONDS,  // Period
            TimeUnit.SECONDS
        );
    }

    /**
     * Send a single heartbeat to Master Node
     * Protocol: HEARTBEAT|nodeName
     */
    private void sendHeartbeat() {
        try (
            Socket socket = new Socket(MASTER_HOST, MASTER_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            // Send heartbeat message
            String heartbeatMessage = "HEARTBEAT|" + nodeName;
            out.println(heartbeatMessage);

            // Wait for acknowledgment
            String response = in.readLine();
            
            if (response != null && response.startsWith("OK")) {
                System.out.println("♥ Heartbeat sent and acknowledged");
            } else {
                System.out.println("⚠ Heartbeat sent but unexpected response: " + response);
            }

        } catch (IOException e) {
            System.err.println("✗ Failed to send heartbeat (Master may be down)");
            // Don't print full stack trace for heartbeat failures to keep logs clean
        }
    }

    /**
     * Graceful shutdown
     */
    public void shutdown() {
        System.out.println("\n⚠ Shutting down Data Node...");
        running = false;

        // Shutdown heartbeat service
        heartbeatService.shutdown();
        try {
            if (!heartbeatService.awaitTermination(3, TimeUnit.SECONDS)) {
                heartbeatService.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatService.shutdownNow();
        }

        System.out.println("✓ Data Node stopped");
    }

    /**
     * Main entry point
     * Modify parameters to run different data nodes
     */
    public static void main(String[] args) {
        // Default to DataNode1 if no arguments provided
        String nodeName = "DataNode1";
        int port = 9101;
        long capacity = 10737418240L; // 10 GB

        // Allow command-line arguments for flexibility
        if (args.length >= 2) {
            nodeName = args[0];
            port = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            capacity = Long.parseLong(args[2]);
        }

        DataNodeServer dataNode = new DataNodeServer(nodeName, "localhost", port, capacity);
        
        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            dataNode.shutdown();
        }));
        
        dataNode.start();
    }
}
