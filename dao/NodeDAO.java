package dao;

import utils.DBConnection;
import shared.NodeInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Node operations.
 *
 * Day 8 addition: getNodeById(nodeId) — used by DownloadServlet to look up
 * the IP and port of a data node given its node_id from chunk_locations.
 */
public class NodeDAO {

    private Connection connection;

    public NodeDAO() {
        this.connection = DBConnection.getInstance().getConnection();
    }

    /**
     * Register a new data node in the database
     */
    public boolean registerNode(NodeInfo node) {
        if (nodeExists(node.getNodeName())) {
            System.out.println("⚠ Node already registered: " + node.getNodeName());
            return updateNode(node);
        }

        String query = "INSERT INTO nodes (node_name, ip_address, port, status, storage_capacity) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, node.getNodeName());
            stmt.setString(2, node.getIpAddress());
            stmt.setInt(3, node.getPort());
            stmt.setString(4, "ACTIVE");
            stmt.setLong(5, node.getStorageCapacity());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        node.setNodeId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("✓ Node registered successfully: " + node.getNodeName()
                        + " (ID: " + node.getNodeId() + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Error registering node: " + node.getNodeName());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update existing node information
     */
    public boolean updateNode(NodeInfo node) {
        String query = "UPDATE nodes SET ip_address = ?, port = ?, status = ?, storage_capacity = ? WHERE node_name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, node.getIpAddress());
            stmt.setInt(2, node.getPort());
            stmt.setString(3, "ACTIVE");
            stmt.setLong(4, node.getStorageCapacity());
            stmt.setString(5, node.getNodeName());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✓ Node updated successfully: " + node.getNodeName());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Error updating node: " + node.getNodeName());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update node status (ACTIVE, DEAD, INACTIVE)
     */
    public boolean updateNodeStatus(int nodeId, String status) {
        String query = "UPDATE nodes SET status = ? WHERE node_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, status);
            stmt.setInt(2, nodeId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✓ Node status updated: ID=" + nodeId + " → " + status);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Error updating node status: ID=" + nodeId);
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update node status by node name
     */
    public boolean updateNodeStatusByName(String nodeName, String status) {
        String query = "UPDATE nodes SET status = ? WHERE node_name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, status);
            stmt.setString(2, nodeName);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✓ Node status updated: " + nodeName + " → " + status);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Error updating node status: " + nodeName);
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get node by name
     */
    public NodeInfo getNodeByName(String nodeName) {
        String query = "SELECT * FROM nodes WHERE node_name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, nodeName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractNodeFromResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Error fetching node: " + nodeName);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Day 8 — Get a node by its node_id.
     *
     * Used by DownloadServlet to resolve a node_id (from chunk_locations table)
     * into an actual IP address and port so we can open a socket to it.
     *
     * @param nodeId the primary key of the node
     * @return NodeInfo if found, null otherwise
     */
    public NodeInfo getNodeById(int nodeId) {
        String query = "SELECT * FROM nodes WHERE node_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, nodeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractNodeFromResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Error fetching node by ID: " + nodeId);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get all active nodes
     */
    public List<NodeInfo> getAllActiveNodes() {
        List<NodeInfo> nodes = new ArrayList<>();
        String query = "SELECT * FROM nodes WHERE status = 'ACTIVE'";

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                nodes.add(extractNodeFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("✗ Error fetching active nodes");
            e.printStackTrace();
        }

        return nodes;
    }

    /**
     * Get all nodes regardless of status
     */
    public List<NodeInfo> getAllNodes() {
        List<NodeInfo> nodes = new ArrayList<>();
        String query = "SELECT * FROM nodes";

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                nodes.add(extractNodeFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("✗ Error fetching all nodes");
            e.printStackTrace();
        }

        return nodes;
    }

    /**
     * Check if node exists by name
     */
    public boolean nodeExists(String nodeName) {
        String query = "SELECT COUNT(*) FROM nodes WHERE node_name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, nodeName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Error checking node existence: " + nodeName);
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Helper method to extract NodeInfo from ResultSet
     */
    private NodeInfo extractNodeFromResultSet(ResultSet rs) throws SQLException {
        NodeInfo node = new NodeInfo();
        node.setNodeId(rs.getInt("node_id"));
        node.setNodeName(rs.getString("node_name"));
        node.setIpAddress(rs.getString("ip_address"));
        node.setPort(rs.getInt("port"));
        node.setStatus(rs.getString("status"));
        node.setStorageCapacity(rs.getLong("storage_capacity"));
        return node;
    }
}