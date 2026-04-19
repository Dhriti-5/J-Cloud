package master;

import dao.ChunkLocationDAO;
import dao.FileDAO;
import dao.NodeDAO;
import shared.NodeInfo;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/**
 * Client Handler - Protocol Parser
 * Runs in thread pool to handle each client connection
 * 
 * Protocol Format (pipe-delimited):
 * - REGISTER_NODE|nodeName|ipAddress|port|capacity
 * - HEARTBEAT|nodeName
 * - PING (returns PONG)
 */
public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private NodeDAO nodeDAO;
    private FileDAO fileDAO;
    private ChunkLocationDAO chunkLocationDAO;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.nodeDAO = new NodeDAO();
        this.fileDAO = new FileDAO();
        this.chunkLocationDAO = new ChunkLocationDAO();
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // Read the request from client
            String request = in.readLine();
            
            if (request == null || request.trim().isEmpty()) {
                System.out.println("⚠ Empty request received");
                return;
            }

            System.out.println("← Received: " + request);

            // Parse the pipe-delimited protocol
            String[] parts = request.split("\\|");
            String command = parts[0];

            // Route to appropriate handler
            switch (command) {
                case "REGISTER_NODE":
                    handleNodeRegistration(parts, out);
                    break;
                
                case "HEARTBEAT":
                    handleHeartbeat(parts, out);
                    break;
                
                case "PING":
                    out.println("PONG");
                    System.out.println("→ Sent: PONG");
                    break;

                case "DELETE_REQUEST":
                    handleDeleteRequest(parts, out);
                    break;
                
                default:
                    out.println("ERROR|Unknown command: " + command);
                    System.out.println("✗ Unknown command: " + command);
            }

        } catch (IOException e) {
            System.err.println("✗ Error handling client request");
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle node registration: REGISTER_NODE|nodeName|ipAddress|port|capacity
     */
    private void handleNodeRegistration(String[] parts, PrintWriter out) {
        if (parts.length < 5) {
            out.println("ERROR|Invalid REGISTER_NODE format");
            System.out.println("✗ Invalid REGISTER_NODE format");
            return;
        }

        try {
            String nodeName = parts[1];
            String ipAddress = parts[2];
            int port = Integer.parseInt(parts[3]);
            long capacity = Long.parseLong(parts[4]);

            // Create NodeInfo object
            NodeInfo node = new NodeInfo(nodeName, ipAddress, port, capacity);

            // Register in database
            boolean success = nodeDAO.registerNode(node);

            if (success) {
                // Store node ID in map for quick lookups
                MasterServer.nodeIdMap.put(nodeName, node.getNodeId());
                
                // Initialize heartbeat timestamp
                MasterServer.lastHeartbeatMap.put(nodeName, System.currentTimeMillis());
                
                out.println("OK|Node registered successfully");
                System.out.println("✓ Node registered: " + nodeName + " (" + ipAddress + ":" + port + ")");
            } else {
                out.println("ERROR|Failed to register node");
                System.out.println("✗ Failed to register node: " + nodeName);
            }

        } catch (NumberFormatException e) {
            out.println("ERROR|Invalid port or capacity value");
            System.out.println("✗ Invalid number format in REGISTER_NODE");
        }
    }

    /**
     * Handle heartbeat: HEARTBEAT|nodeName
     */
    private void handleHeartbeat(String[] parts, PrintWriter out) {
        if (parts.length < 2) {
            out.println("ERROR|Invalid HEARTBEAT format");
            System.out.println("✗ Invalid HEARTBEAT format");
            return;
        }

        String nodeName = parts[1];
        
        // Update heartbeat timestamp
        long currentTime = System.currentTimeMillis();
        MasterServer.lastHeartbeatMap.put(nodeName, currentTime);
        
        out.println("OK|Heartbeat received");
        System.out.println("♥ Heartbeat from: " + nodeName);

        // Update status to ACTIVE in database (if it was marked DEAD)
        Integer nodeId = MasterServer.nodeIdMap.get(nodeName);
        if (nodeId != null) {
            nodeDAO.updateNodeStatus(nodeId, "ACTIVE");
        }
    }

    /**
     * Handle file deletion requests from the web layer.
     * Protocol: DELETE_REQUEST|fileId
     */
    private void handleDeleteRequest(String[] parts, PrintWriter out) {
        if (parts.length < 2) {
            out.println("ERROR|Invalid DELETE_REQUEST format");
            System.out.println("✗ Invalid DELETE_REQUEST format");
            return;
        }

        int fileId;
        try {
            fileId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            out.println("ERROR|Invalid fileId");
            System.out.println("✗ Invalid fileId in DELETE_REQUEST");
            return;
        }

        // Gather locations before metadata delete.
        List<ChunkLocationDAO.ChunkPhysicalLocation> locations =
            chunkLocationDAO.getChunkLocationsByFileId(fileId);

        // Delete metadata first so file disappears from UI immediately.
        boolean deleted = fileDAO.deleteFile(fileId);
        if (!deleted) {
            out.println("ERROR|File not found or delete failed");
            System.out.println("✗ Failed metadata delete for fileId=" + fileId);
            return;
        }

        // Respond immediately; physical deletion runs asynchronously.
        out.println("OK|DELETE_QUEUED|" + locations.size());
        System.out.println("✓ Metadata deleted for fileId=" + fileId
            + ", queued physical deletions=" + locations.size());

        for (ChunkLocationDAO.ChunkPhysicalLocation location : locations) {
            MasterServer.submitBackgroundTask(() -> deleteChunkOnDataNode(location));
        }
    }

    private void deleteChunkOnDataNode(ChunkLocationDAO.ChunkPhysicalLocation location) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(location.getIpAddress(), location.getPort()), 3000);
            socket.setSoTimeout(3000);

            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {
                out.writeUTF("DELETE_CHUNK|" + location.getChunkId());
                out.flush();

                String response = in.readUTF();
                if (response == null || !response.startsWith("OK")) {
                    System.err.println("⚠ Delete chunk not acknowledged by node "
                        + location.getNodeId() + " for chunkId=" + location.getChunkId());
                }
            }
        } catch (IOException e) {
            System.err.println("⚠ Node offline/unreachable, could not delete physical chunkId="
                + location.getChunkId() + " on " + location.getIpAddress() + ":" + location.getPort());
        }
    }
}
