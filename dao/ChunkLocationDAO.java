package dao;

import shared.ChunkLocation;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Data Access Object for chunk location mapping (which node holds a chunk).
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
}