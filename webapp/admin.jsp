<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="shared.User, shared.NodeInfo, java.util.List" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect("login");
        return;
    }

    List<NodeInfo> allNodes = (List<NodeInfo>) request.getAttribute("allNodes");
    Integer totalNodes = (Integer) request.getAttribute("totalNodes");
    Integer aliveNodes = (Integer) request.getAttribute("aliveNodes");
    Integer deadNodes = (Integer) request.getAttribute("deadNodes");
    Long totalCapacity = (Long) request.getAttribute("totalCapacity");

    if (totalNodes == null) totalNodes = 0;
    if (aliveNodes == null) aliveNodes = 0;
    if (deadNodes == null) deadNodes = 0;
    if (totalCapacity == null) totalCapacity = 0L;

    // Format storage capacity
    String capacityStr;
    if (totalCapacity >= 1024L * 1024 * 1024) {
        capacityStr = String.format("%.2f GB", totalCapacity / (1024.0 * 1024 * 1024));
    } else if (totalCapacity >= 1024 * 1024) {
        capacityStr = String.format("%.1f MB", totalCapacity / (1024.0 * 1024));
    } else {
        capacityStr = totalCapacity + " B";
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard - Node Monitoring - J-Cloud</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f5f7fa;
        }
        .navbar {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 15px 30px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }
        .navbar h1 { font-size: 24px; }
        .nav-links {
            display: flex;
            gap: 20px;
            align-items: center;
        }
        .nav-links a, .nav-links span {
            color: white;
            text-decoration: none;
            transition: opacity 0.3s;
        }
        .nav-links a:hover {
            opacity: 0.8;
        }
        .logout-btn {
            background: rgba(255,255,255,0.2);
            color: white;
            border: 1px solid rgba(255,255,255,0.3);
            padding: 8px 20px;
            border-radius: 5px;
            text-decoration: none;
            transition: background 0.3s;
        }
        .logout-btn:hover { background: rgba(255,255,255,0.3); }
        .container { max-width: 1200px; margin: 30px auto; padding: 0 20px; }
        .page-title {
            color: #333;
            margin-bottom: 30px;
        }
        .page-title h2 {
            font-size: 28px;
            color: #667eea;
            margin-bottom: 5px;
        }
        .page-title p {
            color: #666;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }
        .stat-card {
            background: white;
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            border-left: 5px solid #667eea;
            transition: transform 0.3s;
        }
        .stat-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.15);
        }
        .stat-card.critical {
            border-left-color: #ff6b6b;
        }
        .stat-card h3 {
            color: #999;
            font-size: 12px;
            text-transform: uppercase;
            margin-bottom: 10px;
            letter-spacing: 1px;
        }
        .stat-value {
            font-size: 32px;
            font-weight: bold;
            color: #333;
        }
        .stat-card.critical .stat-value {
            color: #ff6b6b;
        }
        .stat-card.success .stat-value {
            color: #51cf66;
        }
        .nodes-section {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .section-title {
            font-size: 20px;
            color: #333;
            margin-bottom: 20px;
            border-bottom: 2px solid #f0f0f0;
            padding-bottom: 10px;
        }
        .nodes-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        .nodes-table th {
            background: #f9f9f9;
            padding: 15px;
            text-align: left;
            font-weight: 600;
            color: #666;
            border-bottom: 2px solid #e0e0e0;
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .nodes-table td {
            padding: 15px;
            border-bottom: 1px solid #f0f0f0;
            color: #333;
        }
        .nodes-table tr:hover {
            background: #fafafa;
        }
        .status-indicator {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
        }
        .status-alive {
            background: #e7f5f0;
            color: #27ae60;
        }
        .status-alive::before {
            content: "🟢";
            font-size: 16px;
        }
        .status-dead {
            background: #ffe7e7;
            color: #c92a2a;
        }
        .status-dead::before {
            content: "🔴";
            font-size: 16px;
        }
        .status-inactive {
            background: #f0f0f0;
            color: #999;
        }
        .status-inactive::before {
            content: "⚫";
            font-size: 16px;
        }
        .node-name {
            font-weight: 600;
            color: #667eea;
        }
        .node-address {
            color: #999;
            font-size: 13px;
        }
        .capacity-bar {
            background: #e0e0e0;
            height: 8px;
            border-radius: 4px;
            overflow: hidden;
            margin-top: 5px;
        }
        .capacity-fill {
            height: 100%;
            background: linear-gradient(90deg, #667eea, #764ba2);
        }
        .empty-state {
            text-align: center;
            padding: 40px 20px;
            color: #999;
        }
        .empty-state-icon {
            font-size: 48px;
            margin-bottom: 15px;
        }
        .alert {
            padding: 15px 20px;
            border-radius: 8px;
            margin-bottom: 20px;
        }
        .alert.warning {
            background: #fff3cd;
            color: #856404;
            border-left: 4px solid #ffc107;
        }
        .alert.danger {
            background: #f8d7da;
            color: #721c24;
            border-left: 4px solid #dc3545;
        }
        @media (max-width: 768px) {
            .stats-grid {
                grid-template-columns: 1fr;
            }
            .nodes-table {
                font-size: 13px;
            }
            .nodes-table th, .nodes-table td {
                padding: 10px;
            }
            .navbar {
                flex-direction: column;
                gap: 15px;
            }
        }
    </style>
</head>
<body>
    <!-- Navigation Bar -->
    <div class="navbar">
        <h1>🏢 J-Cloud Admin</h1>
        <div class="nav-links">
            <a href="dashboard">📊 Dashboard</a>
            <a href="admin">📡 Node Monitoring</a>
            <span>👤 <%= user.getUsername() %></span>
            <a href="logout" class="logout-btn">Logout</a>
        </div>
    </div>

    <!-- Main Content -->
    <div class="container">
        <div class="page-title">
            <h2>Node Failure Detection & Cluster Monitoring</h2>
            <p>Monitor the health and status of all data nodes in the J-Cloud cluster</p>
        </div>

        <!-- Alerts Section -->
        <% if (deadNodes > 0) { %>
            <div class="alert danger">
                ⚠️ Critical: <strong><%= deadNodes %> node(s)</strong> are currently DOWN. Automatic recovery in progress (Day 12).
            </div>
        <% } else if (totalNodes > 0) { %>
            <div class="alert warning">
                ✓ All <%= totalNodes %> node(s) are operational.
            </div>
        <% } %>

        <!-- Statistics Cards -->
        <div class="stats-grid">
            <div class="stat-card success">
                <h3>Total Nodes</h3>
                <div class="stat-value"><%= totalNodes %></div>
            </div>
            <div class="stat-card success">
                <h3>Alive Nodes</h3>
                <div class="stat-value"><%= aliveNodes %></div>
            </div>
            <% if (deadNodes > 0) { %>
                <div class="stat-card critical">
                    <h3>Dead Nodes</h3>
                    <div class="stat-value"><%= deadNodes %></div>
                </div>
            <% } else { %>
                <div class="stat-card">
                    <h3>Dead Nodes</h3>
                    <div class="stat-value">0</div>
                </div>
            <% } %>
            <div class="stat-card">
                <h3>Total Capacity</h3>
                <div class="stat-value" style="font-size: 24px;"><%= capacityStr %></div>
            </div>
        </div>

        <!-- Nodes Table Section -->
        <div class="nodes-section">
            <h3 class="section-title">📡 Cluster Nodes</h3>

            <% if (allNodes != null && !allNodes.isEmpty()) { %>
                <table class="nodes-table">
                    <thead>
                        <tr>
                            <th>Node Name</th>
                            <th>Status</th>
                            <th>Address</th>
                            <th>Storage Capacity</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (NodeInfo node : allNodes) { 
                            String statusClass = "DEAD".equals(node.getStatus()) ? "status-dead" : 
                                               "ACTIVE".equals(node.getStatus()) ? "status-alive" : "status-inactive";
                            String statusText = "DEAD".equals(node.getStatus()) ? "DEAD" : 
                                              "ACTIVE".equals(node.getStatus()) ? "ALIVE" : "INACTIVE";
                            
                            String capacityFormatted;
                            long capacity = node.getStorageCapacity();
                            if (capacity >= 1024L * 1024 * 1024) {
                                capacityFormatted = String.format("%.2f GB", capacity / (1024.0 * 1024 * 1024));
                            } else if (capacity >= 1024 * 1024) {
                                capacityFormatted = String.format("%.1f MB", capacity / (1024.0 * 1024));
                            } else {
                                capacityFormatted = capacity + " B";
                            }
                        %>
                            <tr>
                                <td>
                                    <div class="node-name"><%= node.getNodeName() %></div>
                                    <div class="node-address">ID: <%= node.getNodeId() %></div>
                                </td>
                                <td>
                                    <span class="status-indicator <%= statusClass %>">
                                        <%= statusText %>
                                    </span>
                                </td>
                                <td>
                                    <div class="node-name"><%= node.getIpAddress() %></div>
                                    <div class="node-address">Port: <%= node.getPort() %></div>
                                </td>
                                <td>
                                    <div><%= capacityFormatted %></div>
                                    <div class="capacity-bar">
                                        <div class="capacity-fill" style="width: 60%;"></div>
                                    </div>
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            <% } else { %>
                <div class="empty-state">
                    <div class="empty-state-icon">📭</div>
                    <p>No nodes registered yet. Start some data nodes to see them here.</p>
                </div>
            <% } %>
        </div>

        <!-- Instructions -->
        <div style="margin-top: 40px; padding: 20px; background: #f9f9f9; border-radius: 8px;">
            <h4 style="color: #333; margin-bottom: 10px;">💡 How It Works</h4>
            <ul style="color: #666; line-height: 1.8; margin-left: 20px;">
                <li><strong>Heartbeat Detection:</strong> Master node checks heartbeats every 10 seconds</li>
                <li><strong>Node Timeout:</strong> Nodes not responding for 15+ seconds marked as DEAD</li>
                <li><strong>Database Sync:</strong> Node status automatically updates in PostgreSQL</li>
                <li><strong>Recovery Trigger:</strong> Dead nodes trigger recovery process (Day 12)</li>
                <li><strong>Green 🟢:</strong> Node is healthy and responding</li>
                <li><strong>Red 🔴:</strong> Node is dead or unreachable</li>
            </ul>
        </div>
    </div>

    <script>
        // Auto-refresh every 10 seconds
        setTimeout(function() {
            location.reload();
        }, 10000);
    </script>
</body>
</html>
