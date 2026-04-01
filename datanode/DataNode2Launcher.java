package datanode;

import utils.Config;

/**
 * Launcher for Data Node 2
 * Runs on port 9102
 */
public class DataNode2Launcher {
    public static void main(String[] args) {
        DataNodeServer dataNode2 = new DataNodeServer(
            "DataNode2",
            Config.DATANODE2_HOST,
            Config.DATANODE2_PORT,
            10737418240L  // 10 GB
        );
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            dataNode2.shutdown();
        }));
        
        dataNode2.start();
    }
}
