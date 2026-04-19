package utils;

import dao.NodeDAO;
import shared.NodeInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility methods for live data-node reachability checks.
 */
public final class NodeHealthUtil {

    private static final int CONNECT_TIMEOUT_MS = 1200;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DEAD = "DEAD";

    private NodeHealthUtil() {
    }

    public static boolean isNodeReachable(NodeInfo node) {
        if (node == null || node.getIpAddress() == null || node.getIpAddress().trim().isEmpty() || node.getPort() <= 0) {
            return false;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(node.getIpAddress(), node.getPort()), CONNECT_TIMEOUT_MS);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean isAnyNodeReachable(List<NodeInfo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }

        NodeDAO nodeDAO = new NodeDAO();
        for (NodeInfo node : nodes) {
            boolean reachable = isNodeReachable(node);
            syncNodeStatus(nodeDAO, node, reachable);
            if (reachable) {
                return true;
            }
        }

        return false;
    }

    public static List<NodeInfo> getReachableNodesSortedByCapacity(List<NodeInfo> nodes) {
        List<NodeInfo> reachable = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            return reachable;
        }

        NodeDAO nodeDAO = new NodeDAO();
        for (NodeInfo node : nodes) {
            boolean nodeReachable = isNodeReachable(node);
            syncNodeStatus(nodeDAO, node, nodeReachable);
            if (nodeReachable) {
                reachable.add(node);
            }
        }

        reachable.sort(Comparator.comparingLong(NodeInfo::getStorageCapacity).reversed());
        return reachable;
    }

    private static void syncNodeStatus(NodeDAO nodeDAO, NodeInfo node, boolean reachable) {
        if (node == null) {
            return;
        }

        String effectiveStatus = reachable ? STATUS_ACTIVE : STATUS_DEAD;
        String dbStatus = node.getStatus();
        node.setStatus(effectiveStatus);

        if (nodeDAO != null
                && node.getNodeId() > 0
                && (dbStatus == null || !effectiveStatus.equalsIgnoreCase(dbStatus))) {
            nodeDAO.updateNodeStatus(node.getNodeId(), effectiveStatus);
        }
    }
}