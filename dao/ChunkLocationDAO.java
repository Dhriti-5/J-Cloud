package dao;

import shared.ChunkLocation;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for chunk location mapping (which node holds a chunk).
 *
 * Day 8 addition: getChunkLocations(chunkId) — used by DownloadServlet
 * to find which node(s) hold a given chunk so it can fetch the bytes.
 */
public class ChunkLocationDAO {

    private final Connection connection;

    public ChunkLocationDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    /**
     * Record where a chunk is stored.
     * @param location mapping between chunk and node
     * @return true if successful
     */
    public boolean createChunkLocation(ChunkLocation location) {
        String sql = "INSERT INTO chunk_locations (chunk_id, node_id) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, location.getChunkId());
            stmt.setInt(2, location.getNodeId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("✗ Error inserting chunk location: " + e.getMessage());
            return false;
        }
    }

    /**
     * Day 8 — Get all node locations for a given chunk (supports replicas).
     *
     * Used by DownloadServlet to find which DataNode(s) hold a specific chunk.
     * Returns a list so that if replication is in place, we can try the next
     * replica when a node is down (fault tolerance).
     *
     * @param chunkId the chunk whose location(s) we need
     * @return list of ChunkLocation records (empty list if none found)
     */
    public List<ChunkLocation> getChunkLocations(int chunkId) {
        String sql = "SELECT chunk_id, node_id FROM chunk_locations WHERE chunk_id = ?";
        List<ChunkLocation> locations = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, chunkId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ChunkLocation loc = new ChunkLocation();
                    loc.setChunkId(rs.getInt("chunk_id"));
                    loc.setNodeId(rs.getInt("node_id"));
                    locations.add(loc);
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error fetching locations for chunkId=" + chunkId + ": " + e.getMessage());
        }

        return locations;
    }

    /**
     * Get all physical chunk locations for a file in one query.
     *
     * Used by Master during DELETE_REQUEST to fan out DELETE_CHUNK commands
     * without doing N+1 database lookups.
     */
    public List<ChunkPhysicalLocation> getChunkLocationsByFileId(int fileId) {
        String sql = "SELECT c.chunk_id, n.node_id, n.ip_address, n.port " +
                     "FROM chunks c " +
                     "JOIN chunk_locations cl ON c.chunk_id = cl.chunk_id " +
                     "JOIN nodes n ON cl.node_id = n.node_id " +
                     "WHERE c.file_id = ?";

        List<ChunkPhysicalLocation> locations = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, fileId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    locations.add(new ChunkPhysicalLocation(
                        rs.getInt("chunk_id"),
                        rs.getInt("node_id"),
                        rs.getString("ip_address"),
                        rs.getInt("port")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error fetching chunk physical locations for fileId=" + fileId + ": " + e.getMessage());
        }

        return locations;
    }

    /**
     * Day 10 — Get count of physical replicas for a chunk.
     * 
     * Used by ReplicationManager to determine if a chunk is under-replicated.
     * 
     * @param chunkId the chunk to count
     * @return number of nodes holding this chunk
     */
    public int getChunkLocationCount(int chunkId) {
        String sql = "SELECT COUNT(DISTINCT node_id) as count FROM chunk_locations WHERE chunk_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, chunkId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error counting replica locations for chunkId=" + chunkId + ": " + e.getMessage());
        }

        return 0;
    }

    /**
     * Day 10 — Find all chunks with fewer than 2 replicas.
     * 
     * Used by ReplicationManager to locate under-replicated chunks that need
     * autonomous healing. Returns chunk_id and file_id.
     * 
     * @return list of maps containing chunk_id and file_id
     */
    public List<java.util.Map<String, Integer>> getUnderReplicatedChunks() {
        String sql = "SELECT DISTINCT c.chunk_id, c.file_id " +
                     "FROM chunks c " +
                     "WHERE c.chunk_id IN ( " +
                     "    SELECT chunk_id FROM chunk_locations " +
                     "    GROUP BY chunk_id " +
                     "    HAVING COUNT(DISTINCT node_id) < 2 " +
                     ") " +
                     "LIMIT 100";

        List<java.util.Map<String, Integer>> results = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Integer> map = new java.util.HashMap<>();
                    map.put("chunk_id", rs.getInt("chunk_id"));
                    map.put("file_id", rs.getInt("file_id"));
                    results.add(map);
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error fetching under-replicated chunks: " + e.getMessage());
        }

        return results;
    }

    /**
     * Day 12 — Get system health statistics
     * 
     * Returns counts for dashboard display:
     * - healthy_chunks: chunks with RF >= 2
     * - under_replicated_chunks: chunks with RF < 2
     * 
     * @return map with keys: healthy_chunks, under_replicated_chunks
     */
    public java.util.Map<String, Integer> getSystemHealth() {
        java.util.Map<String, Integer> health = new java.util.HashMap<>();
        
        String sql = "SELECT " +
                     "COUNT(DISTINCT CASE WHEN rep_count >= 2 THEN chunk_id END) as healthy, " +
                     "COUNT(DISTINCT CASE WHEN rep_count < 2 THEN chunk_id END) as under_replicated " +
                     "FROM ( " +
                     "    SELECT chunk_id, COUNT(DISTINCT node_id) as rep_count " +
                     "    FROM chunk_locations " +
                     "    GROUP BY chunk_id " +
                     ") replica_counts";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                health.put("healthy_chunks", rs.getInt("healthy"));
                health.put("under_replicated_chunks", rs.getInt("under_replicated"));
            }
        } catch (SQLException e) {
            System.err.println("✗ Error fetching system health: " + e.getMessage());
            health.put("healthy_chunks", 0);
            health.put("under_replicated_chunks", 0);
        }

        return health;
    }

    /**
     * Day 12 — Delete all chunk_location mappings for a dead node (METADATA PURGE)
     * 
     * This is the critical trigger that wakes up ReplicationManager.
     * When a node dies, its chunk_locations rows are deleted, making chunks
     * appear under-replicated in the database. ReplicationManager then heals them.
     * 
     * @param nodeId the dead node
     * @return number of mappings deleted
     */
    public int deleteChunkLocationsByNodeId(int nodeId) {
        String sql = "DELETE FROM chunk_locations WHERE node_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, nodeId);
            int rowsDeleted = stmt.executeUpdate();
            
            if (rowsDeleted > 0) {
                System.out.println("✓ Purged " + rowsDeleted + " chunk_location mappings for node_id=" + nodeId);
            }
            return rowsDeleted;

        } catch (SQLException e) {
            System.err.println("✗ Error deleting chunk locations for node_id=" + nodeId);
            e.printStackTrace();
        }

        return 0;
    }

    public static class ChunkPhysicalLocation {
        private final int chunkId;
        private final int nodeId;
        private final String ipAddress;
        private final int port;

        public ChunkPhysicalLocation(int chunkId, int nodeId, String ipAddress, int port) {
            this.chunkId = chunkId;
            this.nodeId = nodeId;
            this.ipAddress = ipAddress;
            this.port = port;
        }

        public int getChunkId() {
            return chunkId;
        }

        public int getNodeId() {
            return nodeId;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public int getPort() {
            return port;
        }
    }
}