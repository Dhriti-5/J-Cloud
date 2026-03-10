package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Thread-safe Singleton for MySQL Database Connection
 * Prevents connection pool exhaustion by reusing a single connection
 */
public class DBConnection {

    private static DBConnection instance;
    private Connection connection;
    
    // Database configuration (overridable via environment variables)
    // Supported env vars: JCLOUD_DB_HOST, JCLOUD_DB_PORT, JCLOUD_DB_NAME, JCLOUD_DB_USER, JCLOUD_DB_PASSWORD
    private static final String DB_HOST = getEnvOrDefault("JCLOUD_DB_HOST", "localhost");
    private static final String DB_PORT = getEnvOrDefault("JCLOUD_DB_PORT", "3306");
    private static final String DB_NAME = getEnvOrDefault("JCLOUD_DB_NAME", "jcloud");
    private static final String DB_USER = getEnvOrDefault("JCLOUD_DB_USER", "root");
    private static final String DB_PASSWORD = getEnvOrDefault("JCLOUD_DB_PASSWORD", "Jcloud@db");
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    // Private constructor to prevent instantiation
    private DBConnection() {
        try {
            // Load MySQL JDBC driver
            Class.forName(DB_DRIVER);
            
            // Establish connection with auto-reconnect
            this.connection = DriverManager.getConnection(
                DB_URL + "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                DB_USER,
                DB_PASSWORD
            );
            
            System.out.println("✓ Database connection established successfully");
            System.out.println("  Connected to: " + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + " as " + DB_USER);
            
        } catch (ClassNotFoundException e) {
            System.err.println("✗ MySQL JDBC Driver not found!");
            e.printStackTrace();
            throw new RuntimeException("Failed to load MySQL driver", e);
        } catch (SQLException e) {
            System.err.println("✗ Database connection failed!");
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    /**
     * Thread-safe method to get the singleton instance
     * Uses double-checked locking for performance
     */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Get the database connection
     * Validates and reconnects if connection is closed
     */
    public Connection getConnection() {
        try {
            // Check if connection is still valid
            if (connection == null || connection.isClosed()) {
                System.out.println("⚠ Connection lost. Reconnecting...");
                this.connection = DriverManager.getConnection(
                    DB_URL + "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    DB_USER,
                    DB_PASSWORD
                );
            }
        } catch (SQLException e) {
            System.err.println("✗ Failed to validate/reconnect database connection");
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Close the database connection
     * Should only be called during application shutdown
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("✗ Error closing database connection");
            e.printStackTrace();
        }
    }

    private static String getEnvOrDefault(String envKey, String defaultValue) {
        String value = System.getenv(envKey);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
