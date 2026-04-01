package servlet;

import dao.NodeDAO;
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

/**
 * Admin Servlet - Day 11
 * Displays cluster health and node status monitoring
 * Restricted to admin users only
 */
@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    private NodeDAO nodeDAO;

    @Override
    public void init() throws ServletException {
        nodeDAO = new NodeDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        // Check if user is authenticated
        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        // TODO: Add admin role check in future
        // For now, allow all authenticated users to view admin dashboard

        try {
            // Fetch all nodes (both ALIVE and DEAD)
            List<NodeInfo> allNodes = nodeDAO.getAllNodes();
            
            // Calculate statistics
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

            // Set request attributes
            request.setAttribute("allNodes", allNodes);
            request.setAttribute("totalNodes", totalNodes);
            request.setAttribute("aliveNodes", aliveNodes);
            request.setAttribute("deadNodes", deadNodes);
            request.setAttribute("totalCapacity", totalCapacity);

            System.out.println("✓ Admin dashboard loaded: " + totalNodes + " nodes, " + 
                             aliveNodes + " alive, " + deadNodes + " dead");

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
