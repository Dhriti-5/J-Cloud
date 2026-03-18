package datanode;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
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

    // Storage server configuration
    private ServerSocket storageSocket;
    private ExecutorService storageExecutor;
    private static final String STORAGE_DIR = "storage";

    /**
     * Constructor for Data Node
     */
    public DataNodeServer(String nodeName, String ipAddress, int port, long storageCapacity) {
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        this.port = port;
        this.storageCapacity = storageCapacity;
        this.heartbeatService = Executors.newScheduledThreadPool(1);
        this.storageExecutor = Executors.newFixedThreadPool(10);
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

        // Step 3: Start storage listener for chunk upload/download
        startStorageServer();

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
     * Start a socket server to accept chunk storage requests from the Master Node.
     */
    private void startStorageServer() {
        ensureStorageDirectoryExists();

        try {
            storageSocket = new ServerSocket(port);
            System.out.println("⇨ Storage server listening on port " + port + " (storage dir: " + new File(STORAGE_DIR).getAbsolutePath() + ")");

            Thread acceptThread = new Thread(() -> {
                while (running && !storageSocket.isClosed()) {
                    try {
                        Socket client = storageSocket.accept();
                        storageExecutor.execute(new StorageHandler(client));
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("✗ Error accepting storage connection: " + e.getMessage());
                        }
                    }
                }
            });
            acceptThread.setDaemon(true);
            acceptThread.start();

        } catch (IOException e) {
            System.err.println("✗ Failed to start storage listener on port " + port);
            e.printStackTrace();
        }
    }

    /**
     * Ensure the storage directory exists (creates it if missing).
     */
    private void ensureStorageDirectoryExists() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("⚠ Failed to create storage directory: " + dir.getAbsolutePath());
            }
        }
    }

    /**
     * Handler for incoming client requests to store or retrieve chunks.
     */
    private class StorageHandler implements Runnable {
        private final Socket socket;

        StorageHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                String request = in.readUTF();
                if (request == null || request.isEmpty()) {
                    return;
                }

                String[] parts = request.split("\\|", 6);
                String command = parts[0];

                switch (command) {
                    case "STORE_CHUNK":
                        handleStoreChunk(parts, in, out);
                        break;
                    case "GET_CHUNK":
                        handleGetChunk(parts, out);
                        break;
                    default:
                        out.writeUTF("ERROR|Unknown command");
                        break;
                }

            } catch (IOException e) {
                System.err.println("✗ Storage handler error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void handleStoreChunk(String[] parts, DataInputStream in, DataOutputStream out) throws IOException {
            if (parts.length < 6) {
                out.writeUTF("ERROR|Invalid STORE_CHUNK format");
                return;
            }

            int chunkId = Integer.parseInt(parts[1]);
            int chunkIndex = Integer.parseInt(parts[2]);
            int fileId = Integer.parseInt(parts[3]);
            int chunkSize = Integer.parseInt(parts[4]);
            String fileName = parts[5];

            byte[] chunkData = new byte[chunkSize];
            in.readFully(chunkData);

            File chunkFile = new File(STORAGE_DIR, String.format("chunk_%d_%d_%d.dat", fileId, chunkIndex, chunkId));
            try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                fos.write(chunkData);
                fos.flush();
            }

            out.writeUTF("OK|STORED");
        }

        private void handleGetChunk(String[] parts, DataOutputStream out) throws IOException {
            if (parts.length < 2) {
                out.writeUTF("ERROR|Invalid GET_CHUNK format");
                return;
            }

            int chunkId = Integer.parseInt(parts[1]);
            File[] matches = new File(STORAGE_DIR).listFiles((dir, name) -> name.contains("_" + chunkId + "."));

            if (matches == null || matches.length == 0) {
                out.writeUTF("ERROR|Chunk not found");
                return;
            }

            File chunkFile = matches[0];
            byte[] data = new byte[(int) chunkFile.length()];
            try (FileInputStream fis = new FileInputStream(chunkFile)) {
                fis.read(data);
            }

            out.writeUTF("OK|" + data.length);
            out.write(data);
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

        // Shutdown storage listener
        if (storageSocket != null && !storageSocket.isClosed()) {
            try {
                storageSocket.close();
            } catch (IOException ignored) {
            }
        }

        if (storageExecutor != null) {
            storageExecutor.shutdown();
            try {
                if (!storageExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    storageExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                storageExecutor.shutdownNow();
            }
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
