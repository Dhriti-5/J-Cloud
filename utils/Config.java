package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration management for J-Cloud.
 *
 * Reads sensitive settings from environment variables to keep secrets
 * out of version control while allowing flexible deployment.
 *
 * Usage:
 *   - Set environment variables before starting servers/servlets
 *   - Or define required values in the .env file at project root
 */
public class Config {

    private static final Map<String, String> DOTENV_VARS = loadDotEnv();

    // Master Node Configuration
    public static final String MASTER_HOST = getRequiredEnv("JCLOUD_MASTER_HOST");
    public static final int MASTER_PORT = getRequiredEnvInt("JCLOUD_MASTER_PORT");

    // Data Node Configuration (Strictly fetched from .env or OS)
    public static final String DATANODE1_HOST = getRequiredEnv("JCLOUD_DATANODE1_HOST");
    public static final String DATANODE2_HOST = getRequiredEnv("JCLOUD_DATANODE2_HOST");

    // Node Ports
    public static final int DATANODE1_PORT = getRequiredEnvInt("JCLOUD_DATANODE1_PORT");
    public static final int DATANODE2_PORT = getRequiredEnvInt("JCLOUD_DATANODE2_PORT");

    /**
     * Get environment variable from OS env first, then .env map.
     */
    private static String lookupValue(String key) {
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        String dotenvValue = DOTENV_VARS.get(key);
        if (dotenvValue != null && !dotenvValue.trim().isEmpty()) {
            return dotenvValue.trim();
        }

        return null;
    }

    /**
     * Get required environment variable as String.
     */
    public static String getRequiredEnv(String key) {
        String value = lookupValue(key);
        if (value == null) {
            throw new IllegalStateException("Missing required config: " + key + " (set in OS env or .env file)");
        }
        return value;
    }

    /**
     * Get optional environment variable as String with explicit fallback.
     */
    public static String getOptionalEnv(String key, String fallbackValue) {
        String value = lookupValue(key);
        if (value == null) {
            return fallbackValue;
        }
        return value;
    }

    /**
     * Load key=value pairs from .env in the project working directory.
     */
    private static Map<String, String> loadDotEnv() {
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            return Collections.emptyMap();
        }

        Map<String, String> values = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }

                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();

                // Strip matching surrounding quotes if present.
                if (value.length() >= 2) {
                    boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
                    boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
                    if (doubleQuoted || singleQuoted) {
                        value = value.substring(1, value.length() - 1);
                    }
                }

                values.put(key, value);
            }
            return values;
        } catch (IOException e) {
            System.err.println("Warning: Failed to read .env file: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Get required environment variable as Integer.
     */
    public static int getRequiredEnvInt(String key) {
        String value = getRequiredEnv(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid integer config for " + key + ": " + value, e);
        }
    }

    /**
     * Get environment variable as Long with explicit fallback.
     */
    public static long getOptionalEnvLong(String key, long fallbackValue) {
        String value = lookupValue(key);
        if (value == null) {
            return fallbackValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid long config for " + key + ": " + value, e);
        }
    }

    /**
     * Print current configuration (safe to call; prints only non-sensitive config).
     */
    public static void printConfig() {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║  J-CLOUD CONFIGURATION                    ║");
        System.out.println("╠═══════════════════════════════════════════╣");
        System.out.println("║  Master Node Host: " + String.format("%-22s", MASTER_HOST) + "║");
        System.out.println("║  Master Node Port: " + String.format("%-22d", MASTER_PORT) + "║");
        System.out.println("║  DataNode 1 Host: " + String.format("%-22s", DATANODE1_HOST) + "║");
        System.out.println("║  DataNode 1 Port: " + String.format("%-22d", DATANODE1_PORT) + "║");
        System.out.println("║  DataNode 2 Host: " + String.format("%-22s", DATANODE2_HOST) + "║");
        System.out.println("║  DataNode 2 Port: " + String.format("%-22d", DATANODE2_PORT) + "║");
        System.out.println("╚═══════════════════════════════════════════╝");
    }
}
