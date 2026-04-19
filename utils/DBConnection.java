package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe Singleton for PostgreSQL Database Connection
 * Prevents connection pool exhaustion by reusing a single connection
 */
public class DBConnection {

    private static final class DbConfig {
        private final String url;
        private final String user;
        private final String password;

        private DbConfig(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }
    }

    private static DBConnection instance;
    private Connection connection;
    private static final Map<String, String> ENV_FILE_VALUES = loadDotEnv();
    private static final DbConfig DB_CONFIG = resolveDbConfig();
    
    // Database configuration (overridable via environment variables)
    // Supported keys in process env or .env: JCLOUD_DB_URL, JCLOUD_DB_HOST, JCLOUD_DB_PORT, JCLOUD_DB_NAME, JCLOUD_DB_USER, JCLOUD_DB_PASSWORD
    private static final String DB_HOST = getConfigOrDefault("JCLOUD_DB_HOST", "localhost");
    private static final String DB_PORT = getConfigOrDefault("JCLOUD_DB_PORT", "5432");
    private static final String DB_NAME = getConfigOrDefault("JCLOUD_DB_NAME", "jcloud_db");
    private static final String DB_USER = DB_CONFIG.user;
    private static final String DB_PASSWORD = DB_CONFIG.password;
    private static final String DB_URL = DB_CONFIG.url;
    private static final String DB_DRIVER = "org.postgresql.Driver";

    // Private constructor to prevent instantiation
    private DBConnection() {
        try {
            // Load PostgreSQL JDBC driver
            Class.forName(DB_DRIVER);

            this.connection = openConnection();
            
            System.out.println("✓ Database connection established successfully");
            System.out.println("  Connected to: " + maskUrl(DB_URL));
            
        } catch (ClassNotFoundException e) {
            System.err.println("✗ PostgreSQL JDBC Driver not found!");
            e.printStackTrace();
            throw new RuntimeException("Failed to load PostgreSQL driver", e);
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
                this.connection = openConnection();
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

    private static String getConfig(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = ENV_FILE_VALUES.get(key);
        }
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private static String getConfigOrDefault(String key, String defaultValue) {
        String value = getConfig(key);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    private static String buildDbUrl() {
        String configuredUrl = getConfig("JCLOUD_DB_URL");
        if (configuredUrl != null && !configuredUrl.trim().isEmpty()) {
            return normalizeJdbcUrl(configuredUrl.trim());
        }

        return "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?sslmode=require";
    }

    private static DbConfig resolveDbConfig() {
        String configuredUrl = getConfig("JCLOUD_DB_URL");
        String envUser = getConfig("JCLOUD_DB_USER");
        String envPassword = getConfig("JCLOUD_DB_PASSWORD");

        String finalUrl = buildDbUrl();
        String finalUser = isPlaceholderCredential(envUser) ? null : envUser;
        String finalPassword = isPlaceholderCredential(envPassword) ? null : envPassword;

        if (configuredUrl != null && !configuredUrl.trim().isEmpty()) {
            String[] urlCreds = extractCredentialsFromUrl(configuredUrl.trim());
            // URL credentials take precedence over external env values to avoid stale overrides in app servers.
            if (urlCreds[0] != null && !urlCreds[0].isEmpty()) {
                finalUser = urlCreds[0];
            }
            if (urlCreds[1] != null && !urlCreds[1].isEmpty()) {
                finalPassword = urlCreds[1];
            }
        }

        return new DbConfig(finalUrl, finalUser, finalPassword);
    }

    private static Connection openConnection() throws SQLException {
        if (DB_USER != null && !DB_USER.trim().isEmpty()) {
            return DriverManager.getConnection(DB_URL, DB_USER.trim(), DB_PASSWORD == null ? "" : DB_PASSWORD.trim());
        }
        return DriverManager.getConnection(DB_URL);
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<>();
        File dotEnvFile = new File(".env");
        if (!dotEnvFile.exists() || !dotEnvFile.isFile()) {
            return values;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(dotEnvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, separator).trim();
                String rawValue = trimmed.substring(separator + 1).trim();
                values.put(key, stripQuotes(rawValue));
            }
        } catch (IOException e) {
            System.err.println("⚠ Could not read .env file: " + e.getMessage());
        }

        return values;
    }

    private static String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String maskUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        String masked = url.replaceAll("(?i)(password=)[^&]+", "$1****");

        int schemeIdx = masked.indexOf("://");
        int atIdx = masked.indexOf('@');
        if (schemeIdx > -1 && atIdx > schemeIdx) {
            String authPart = masked.substring(schemeIdx + 3, atIdx);
            int colonIdx = authPart.indexOf(':');
            if (colonIdx > -1) {
                String safeAuth = authPart.substring(0, colonIdx + 1) + "****";
                masked = masked.substring(0, schemeIdx + 3) + safeAuth + masked.substring(atIdx);
            }
        }

        return masked;
    }

    private static String normalizeJdbcUrl(String url) {
        if (url.startsWith("jdbc:")) {
            return url;
        }

        if (url.startsWith("postgresql://") || url.startsWith("postgres://")) {
            try {
                String canonical = url.startsWith("postgres://")
                    ? "postgresql://" + url.substring("postgres://".length())
                    : url;

                URI uri = URI.create(canonical);
                String host = uri.getHost();
                int port = uri.getPort();
                String path = uri.getRawPath();
                String query = uri.getRawQuery();

                if (host != null && !host.isEmpty()) {
                    StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host);
                    if (port > 0) {
                        jdbc.append(':').append(port);
                    }
                    if (path == null || path.isEmpty()) {
                        jdbc.append('/');
                    } else {
                        jdbc.append(path);
                    }
                    if (query != null && !query.isEmpty()) {
                        jdbc.append('?').append(query);
                    }
                    return jdbc.toString();
                }
            } catch (IllegalArgumentException ignored) {
                // Fall back to previous behavior below.
            }

            return "jdbc:" + url;
        }

        return url;
    }

    private static String[] extractCredentialsFromUrl(String url) {
        String[] creds = new String[] { null, null };

        try {
            String canonical = url;
            if (url.startsWith("postgres://")) {
                canonical = "postgresql://" + url.substring("postgres://".length());
            }

            URI uri = URI.create(canonical);
            String userInfo = uri.getRawUserInfo();
            if (userInfo == null || userInfo.isEmpty()) {
                return creds;
            }

            int split = userInfo.indexOf(':');
            if (split >= 0) {
                creds[0] = decodeUrlComponent(userInfo.substring(0, split));
                creds[1] = decodeUrlComponent(userInfo.substring(split + 1));
            } else {
                creds[0] = decodeUrlComponent(userInfo);
            }
        } catch (IllegalArgumentException ignored) {
            return creds;
        }

        return creds;
    }

    private static String decodeUrlComponent(String value) {
        if (value == null) {
            return null;
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean isPlaceholderCredential(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.startsWith("your_") || normalized.equals("your_neon_user") || normalized.equals("your_neon_password");
    }
}
