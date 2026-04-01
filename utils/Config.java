package utils;

/**
 * Configuration management for J-Cloud.
 *
 * Reads sensitive settings from environment variables to keep secrets
 * out of version control while allowing flexible deployment.
 *
 * Usage:
 *   - Set environment variables before starting servers/servlets
 *   - Fallback defaults are provided for development (localhost)
 */
public class Config {

    // Master Node Configuration
    public static final String MASTER_HOST = getEnv("JCLOUD_MASTER_HOST", "localhost");
    public static final int MASTER_PORT = getEnvInt("JCLOUD_MASTER_PORT", 9000);

    // Data Node Configuration (per node - set via command-line args or env vars)
    public static final String DATANODE_HOST = getEnv("JCLOUD_DATANODE_HOST", "localhost");
    public static final String DATANODE1_HOST = getEnv("JCLOUD_DATANODE1_HOST", DATANODE_HOST);
    public static final String DATANODE2_HOST = getEnv("JCLOUD_DATANODE2_HOST", DATANODE_HOST);

    // Node Ports
    public static final int DATANODE1_PORT = getEnvInt("JCLOUD_DATANODE1_PORT", 9101);
    public static final int DATANODE2_PORT = getEnvInt("JCLOUD_DATANODE2_PORT", 9102);

    /**
     * Get environment variable as String with fallback default.
     */
    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        return defaultValue;
    }

    /**
     * Get environment variable as Integer with fallback default.
     */
    public static int getEnvInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                System.err.println("⚠ Warning: " + key + " is not a valid integer, using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Get environment variable as Long with fallback default.
     */
    public static long getEnvLong(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                System.err.println("⚠ Warning: " + key + " is not a valid long, using default: " + defaultValue);
            }
        }
        return defaultValue;
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
