package servlet;

import dao.NodeDAO;
import dao.EventLogDAO;
import dao.ChunkLocationDAO;
import shared.NodeInfo;
import shared.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Admin Servlet - Day 11 & 12
 * Displays cluster health, node status, event logs, and recovery progress
 * 
 * Dashboard Integration Points:
 * - Node Status: from nodes table (Day 11)
 * - Event Logs: from event_logs table (Day 12)
 * - System Health: under-replicated vs healthy chunks (Day 12)
 */
@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    private NodeDAO nodeDAO;
    private EventLogDAO eventLogDAO;      // Day 12: Event logging
    private ChunkLocationDAO chunkLocationDAO;  // Day 12: System health

    @Override
    public void init() throws ServletException {
        nodeDAO = new NodeDAO();
        eventLogDAO = new EventLogDAO();
        chunkLocationDAO = new ChunkLocationDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));

        // Check if user is authenticated
        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        // Only fixed admin credentials are allowed to view admin dashboard
        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: admin credentials required");
            return;
        }

        try {
            // **TASK 1: Fetch Node Status (Day 11)**
            List<NodeInfo> allNodes = nodeDAO.getAllNodes();
            
            int totalNodes = (allNodes != null) ? allNodes.size() : 0;
            int aliveNodes = 0;
            int deadNodes = 0;
            long totalCapacity = 0L;

            if (allNodes != null) {
                for (NodeInfo node : allNodes) {
                    totalCapacity += node.getStorageCapacity();
                    
                    if ("DEAD".equals(node.getStatus())) {
                        deadNodes++;
                    } else if ("ACTIVE".equals(node.getStatus())) {
                        aliveNodes++;
                    }
                }
            }

            // **TASK 2: Fetch Event Logs (Day 12)**
            List<Map<String, String>> recentEvents = eventLogDAO.getRecentEvents(20);
            
            // **TASK 3: Fetch System Health (Day 12)**
            Map<String, Integer> systemHealth = chunkLocationDAO.getSystemHealth();
            
            int healthyChunks = systemHealth.getOrDefault("healthy_chunks", 0);
            int underReplicatedChunks = systemHealth.getOrDefault("under_replicated_chunks", 0);
            int totalChunks = healthyChunks + underReplicatedChunks;

            // Set request attributes for JSP
            request.setAttribute("allNodes", allNodes);
            request.setAttribute("totalNodes", totalNodes);
            request.setAttribute("aliveNodes", aliveNodes);
            request.setAttribute("deadNodes", deadNodes);
            request.setAttribute("totalCapacity", totalCapacity);
            
            request.setAttribute("recentEvents", recentEvents);
            request.setAttribute("healthyChunks", healthyChunks);
            request.setAttribute("underReplicatedChunks", underReplicatedChunks);
            request.setAttribute("totalChunks", totalChunks);

            System.out.println("✓ Admin dashboard loaded:");
            System.out.println("  - Nodes: " + totalNodes + " (" + aliveNodes + " alive, " + deadNodes + " dead)");
            System.out.println("  - Chunks: " + totalChunks + " (" + healthyChunks + " healthy, " + 
                             underReplicatedChunks + " under-replicated)");
            System.out.println("  - Recent events: " + recentEvents.size());

            // Forward to admin JSP
            request.getRequestDispatcher("/admin.jsp").forward(request, response);

        } catch (Exception e) {
            System.err.println("✗ Error loading admin dashboard");
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                             "Error loading cluster status");
        }
    }
}
