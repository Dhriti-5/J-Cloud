package dao;

import shared.FileMetadata;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for file metadata.
 */
public class FileDAO {

    private final Connection connection;

    public FileDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    /**
     * Create a new file record.
     * @param file File metadata to insert
     * @return true if inserted successfully, false otherwise
     */
    public boolean createFile(FileMetadata file) {
        String sql = "INSERT INTO files (file_name, file_size, owner_id) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, file.getFileName());
            stmt.setLong(2, file.getFileSize());
            stmt.setInt(3, file.getOwnerId());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                return false;
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    file.setFileId(rs.getInt(1));
                }
            }

            return true;
        } catch (SQLException e) {
            System.err.println("✗ Error inserting file metadata: " + e.getMessage());
            return false;
        }
    }

    /**
     * List all files belonging to a user.
     */
    public List<FileMetadata> listFilesByOwner(int ownerId) {
        String sql = "SELECT file_id, file_name, file_size, owner_id FROM files WHERE owner_id = ? ORDER BY upload_time DESC";
        List<FileMetadata> files = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ownerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FileMetadata file = new FileMetadata();
                    file.setFileId(rs.getInt("file_id"));
                    file.setFileName(rs.getString("file_name"));
                    file.setFileSize(rs.getLong("file_size"));
                    file.setOwnerId(rs.getInt("owner_id"));
                    files.add(file);
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error listing files for ownerId=" + ownerId + ": " + e.getMessage());
        }

        return files;
    }

    /**
     * Get a file metadata record by its ID.
     */
    public FileMetadata getFileById(int fileId) {
        String sql = "SELECT file_id, file_name, file_size, owner_id FROM files WHERE file_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    FileMetadata file = new FileMetadata();
                    file.setFileId(rs.getInt("file_id"));
                    file.setFileName(rs.getString("file_name"));
                    file.setFileSize(rs.getLong("file_size"));
                    file.setOwnerId(rs.getInt("owner_id"));
                    return file;
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Error fetching file by ID: " + e.getMessage());
        }

        return null;
    }
}