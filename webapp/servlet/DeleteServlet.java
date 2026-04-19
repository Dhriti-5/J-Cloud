package servlet;

import dao.FileDAO;
import dao.NodeDAO;
import shared.FileMetadata;
import shared.NodeInfo;
import shared.User;
import utils.Config;
import utils.NodeHealthUtil;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Day 9 - Scalable delete entry point.
 *
 * Web flow:
 * 1) Authenticate user
 * 2) Validate ownership for file_id
 * 3) Send DELETE_REQUEST|fileId to Master
 * 4) Redirect immediately without waiting for physical chunk deletion
 */
@WebServlet("/delete")
public class DeleteServlet extends HttpServlet {

    private static final String MASTER_HOST = Config.MASTER_HOST;
    private static final int MASTER_PORT = Config.MASTER_PORT;

    private FileDAO fileDAO;
    private NodeDAO nodeDAO;

    @Override
    public void init() throws ServletException {
        fileDAO = new FileDAO();
        nodeDAO = new NodeDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login");
            return;
        }

        User user = (User) session.getAttribute("user");

        String fileIdParam = request.getParameter("file_id");
        if (fileIdParam == null || fileIdParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/files.jsp");
            return;
        }

        int fileId;
        try {
            fileId = Integer.parseInt(fileIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/files.jsp");
            return;
        }

        FileMetadata file = fileDAO.getFileById(fileId);
        if (file == null || file.getOwnerId() != user.getUserId()) {
            response.sendRedirect(request.getContextPath() + "/files.jsp");
            return;
        }

        // Check if at least one node is reachable right now.
        List<NodeInfo> allNodes = nodeDAO.getAllNodes();
        if (!NodeHealthUtil.isAnyNodeReachable(allNodes)) {
            request.setAttribute("error",
                "Server is offline: all data nodes are inactive. Delete is disabled until at least one node is online.");
            request.getRequestDispatcher("/files.jsp").forward(request, response);
            return;
        }

        // Trigger metadata-first delete on Master; physical delete is async.
        try (Socket socket = new Socket(MASTER_HOST, MASTER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("DELETE_REQUEST|" + fileId);
            in.readLine(); // optional ack, ignored to keep UX non-blocking

        } catch (IOException e) {
            System.err.println("⚠ Failed to submit DELETE_REQUEST to Master for fileId=" + fileId + ": " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/dashboard.jsp");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Force deletion through POST from UI actions.
        response.sendRedirect(request.getContextPath() + "/files.jsp");
    }
}
