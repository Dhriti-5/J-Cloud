package servlet;

import dao.UserDAO;
import shared.User;
import utils.Config;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Login Servlet
 * Handles user authentication and session management
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String ADMIN_USERNAME_KEY = "JCLOUD_ADMIN_USERNAME";
    private static final String ADMIN_PASSWORD_KEY = "JCLOUD_ADMIN_PASSWORD";
    private static final String ADMIN_EMAIL_KEY = "JCLOUD_ADMIN_EMAIL";

    private String adminUsername;
    private String adminPassword;
    private String adminEmail;

    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
        try {
            adminUsername = Config.getRequiredEnv(ADMIN_USERNAME_KEY);
            adminPassword = Config.getRequiredEnv(ADMIN_PASSWORD_KEY);
            adminEmail = Config.getRequiredEnv(ADMIN_EMAIL_KEY);
        } catch (IllegalStateException ex) {
            throw new ServletException("Missing admin login configuration in environment/.env", ex);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check if user just registered
        String registered = request.getParameter("registered");
        if ("true".equals(registered)) {
            request.setAttribute("success", "Registration successful! Please login.");
        }

        // Forward to login page
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Extract form parameters
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Validation
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "Username and password are required");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        // Fixed admin login (independent from database users)
        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            User adminUser = new User();
            adminUser.setUserId(-1);
            adminUser.setUsername(adminUsername);
            adminUser.setEmail(adminEmail);

            HttpSession session = request.getSession(true);
            session.setAttribute("user", adminUser);
            session.setAttribute("username", adminUser.getUsername());
            session.setAttribute("userId", adminUser.getUserId());
            session.setAttribute("isAdmin", true);
            session.setAttribute("userEmail", adminEmail);
            session.setMaxInactiveInterval(30 * 60);

            System.out.println("✓ Admin logged in: " + adminUsername);
            response.sendRedirect("admin");
            return;
        }

        // Hash password for comparison
        String hashedPassword = hashPassword(password);

        // Authenticate user
        User user = userDAO.authenticateUser(username, hashedPassword);

        if (user != null) {
            // Authentication successful - create session
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("username", user.getUsername());
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("isAdmin", false);
            session.setAttribute("userEmail", user.getEmail());
            
            // Session timeout: 30 minutes
            session.setMaxInactiveInterval(30 * 60);

            System.out.println("✓ User logged in: " + username);

            // Redirect to dashboard
            response.sendRedirect("dashboard.jsp");
        } else {
            // Authentication failed
            request.setAttribute("error", "Invalid username or password");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    /**
     * Simple MD5 password hashing (use BCrypt in production)
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(password.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("⚠ Password hashing failed, using plain text");
            return password;
        }
    }
}
