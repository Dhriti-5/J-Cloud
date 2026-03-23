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
}