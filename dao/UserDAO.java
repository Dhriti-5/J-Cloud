package dao;

import utils.DBConnection;
import shared.User;

import java.sql.*;

/**
 * Data Access Object for User operations
 * Thread-safe database operations for user authentication and registration
 */
public class UserDAO {

    private Connection connection;

    public UserDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    /**
     * Register a new user in the database
     * @param user User object containing username, password, and email
     * @return true if registration successful, false otherwise
     */
    public boolean registerUser(User user) {
        String query = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword()); // TODO: Hash password in production
            stmt.setString(3, user.getEmail());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Retrieve the auto-generated user_id
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("✓ User registered successfully: " + user.getUsername());
                return true;
            }
            
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("✗ Username already exists: " + user.getUsername());
        } catch (SQLException e) {
            System.err.println("✗ Error registering user: " + user.getUsername());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Authenticate user credentials
     * @param username User's username
     * @param password User's password
     * @return User object if authentication successful, null otherwise
     */
    public User authenticateUser(String username, String password) {
        String query = "SELECT user_id, username, email FROM users WHERE username = ? AND password = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password); // TODO: Compare hashed password in production
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    
                    System.out.println("✓ User authenticated: " + username);
                    return user;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error authenticating user: " + username);
            e.printStackTrace();
        }
        
        System.out.println("✗ Invalid credentials for: " + username);
        return null;
    }

    /**
     * Get user by username
     * @param username User's username
     * @return User object if found, null otherwise
     */
    public User getUserByUsername(String username) {
        String query = "SELECT user_id, username, email, created_at FROM users WHERE username = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    return user;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error fetching user: " + username);
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Check if username already exists
     * @param username Username to check
     * @return true if exists, false otherwise
     */
    public boolean usernameExists(String username) {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error checking username existence: " + username);
            e.printStackTrace();
        }
        
        return false;
    }
}
