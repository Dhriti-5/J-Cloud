package dao;

import shared.Chunk;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for file chunk metadata.
 */
public class ChunkDAO {

    private final Connection connection;

    public ChunkDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    /**
     * Insert a new chunk record and return generated chunk_id.
     */
    public int createChunk(Chunk chunk) {
        String sql = "INSERT INTO chunks (file_id, chunk_index, chunk_size) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, chunk.getFileId());
            stmt.setInt(2, chunk.getChunkIndex());
            stmt.setInt(3, chunk.getChunkSize());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                return -1;
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int chunkId = rs.getInt(1);
                    chunk.setChunkId(chunkId);
                    return chunkId;
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error inserting chunk metadata: " + e.getMessage());
        }

        return -1;
    }

    /**
     * Get all chunks for a file ordered by index.
     */
    public List<Chunk> getChunksByFileId(int fileId) {
        String sql = "SELECT chunk_id, file_id, chunk_index, chunk_size FROM chunks WHERE file_id = ? ORDER BY chunk_index";
        List<Chunk> chunks = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Chunk chunk = new Chunk();
                    chunk.setChunkId(rs.getInt("chunk_id"));
                    chunk.setFileId(rs.getInt("file_id"));
                    chunk.setChunkIndex(rs.getInt("chunk_index"));
                    chunk.setChunkSize(rs.getInt("chunk_size"));
                    chunks.add(chunk);
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error fetching chunks for fileId=" + fileId + ": " + e.getMessage());
        }

        return chunks;
    }
}