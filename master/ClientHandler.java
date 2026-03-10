package master;

import dao.NodeDAO;
import shared.NodeInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.nodeDAO = new NodeDAO();
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
}
