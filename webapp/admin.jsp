<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="shared.User, shared.NodeInfo, java.util.List, java.util.Map" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect("login");
        return;
    }

    // Day 11: Node Status
    List<NodeInfo> allNodes = (List<NodeInfo>) request.getAttribute("allNodes");
    Integer totalNodes = (Integer) request.getAttribute("totalNodes");
    Integer aliveNodes = (Integer) request.getAttribute("aliveNodes");
    Integer deadNodes = (Integer) request.getAttribute("deadNodes");
    Long totalCapacity = (Long) request.getAttribute("totalCapacity");

    // Day 12: Event Logs & System Health
    List<Map<String, String>> recentEvents = (List<Map<String, String>>) request.getAttribute("recentEvents");
    Integer healthyChunks = (Integer) request.getAttribute("healthyChunks");
    Integer underReplicatedChunks = (Integer) request.getAttribute("underReplicatedChunks");
    Integer totalChunks = (Integer) request.getAttribute("totalChunks");

    if (totalNodes == null) totalNodes = 0;
    if (aliveNodes == null) aliveNodes = 0;
    if (deadNodes == null) deadNodes = 0;
    if (totalCapacity == null) totalCapacity = 0L;
    if (recentEvents == null) recentEvents = new java.util.ArrayList<>();
    if (healthyChunks == null) healthyChunks = 0;
    if (underReplicatedChunks == null) underReplicatedChunks = 0;
    if (totalChunks == null) totalChunks = 0;

    // Format storage capacity
    String capacityStr;
    if (totalCapacity >= 1024L * 1024 * 1024) {
        capacityStr = String.format("%.2f GB", totalCapacity / (1024.0 * 1024 * 1024));
    } else if (totalCapacity >= 1024 * 1024) {
        capacityStr = String.format("%.1f MB", totalCapacity / (1024.0 * 1024));
    } else {
        capacityStr = totalCapacity + " B";
    }
    
    // Calculate recovery progress percentage
    int recoveryPercent = totalChunks > 0 ? (healthyChunks * 100) / totalChunks : 100;
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
        .alert.success {
            background: #d4edda;
            color: #155724;
            border-left: 4px solid #28a745;
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

        <!-- Day 12: System Health Stats -->
        <div class="stats-grid" style="margin-top: 20px;">
            <div class="stat-card success">
                <h3>Healthy Chunks</h3>
                <div class="stat-value"><%= healthyChunks %></div>
                <p style="color: #51cf66; font-size: 12px; margin-top: 8px;">RF ≥ 2</p>
            </div>
            <% if (underReplicatedChunks > 0) { %>
                <div class="stat-card critical">
                    <h3>Under-Replicated</h3>
                    <div class="stat-value"><%= underReplicatedChunks %></div>
                    <p style="color: #ff6b6b; font-size: 12px; margin-top: 8px;">🔄 Repairing...</p>
                </div>
            <% } else { %>
                <div class="stat-card success">
                    <h3>Under-Replicated</h3>
                    <div class="stat-value">0</div>
                    <p style="color: #51cf66; font-size: 12px; margin-top: 8px;">All healthy</p>
                </div>
            <% } %>
            <div class="stat-card">
                <h3>Recovery Progress</h3>
                <div class="stat-value" style="font-size: 24px;"><%= recoveryPercent %>%</div>
                <div style="background: #e0e0e0; height: 6px; border-radius: 3px; margin-top: 8px;">
                    <div style="background: linear-gradient(90deg, #51cf66, #40c057); width: <%= recoveryPercent %>%; height: 100%; border-radius: 3px;"></div>
                </div>
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

        <!-- Day 12: Event Log Section -->
        <div style="background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); margin-top: 40px;">
            <h3 class="section-title">📋 Live Event Feed (Day 12)</h3>
            <p style="color: #666; font-size: 13px; margin-bottom: 20px;">System recovery and replication events in real-time</p>
            
            <% if (recentEvents != null && !recentEvents.isEmpty()) { %>
                <div style="max-height: 400px; overflow-y: auto;">
                    <% for (Map<String, String> event : recentEvents) {
                        String eventType = event.getOrDefault("event_type", "UNKNOWN");
                        String message = event.getOrDefault("message", "");
                        String createdAt = event.getOrDefault("created_at", "");
                        
                        // Determine alert style based on event type
                        String alertClass = "warning";
                        String icon = "ℹ️";
                        if (eventType.contains("FAILURE")) {
                            alertClass = "danger";
                            icon = "❌";
                        } else if (eventType.contains("SUCCESS")) {
                            alertClass = "success";
                            icon = "✅";
                        } else if (eventType.contains("RECOVERY")) {
                            alertClass = "success";
                            icon = "🔄";
                        } else if (eventType.contains("PURGE")) {
                            alertClass = "warning";
                            icon = "🧹";
                        }
                    %>
                        <div style="display: flex; gap: 10px; padding: 12px; margin-bottom: 10px; background: <%= 
                            "danger".equals(alertClass) ? "#ffe7e7" : 
                            "success".equals(alertClass) ? "#e7f5f0" : 
                            "#fff3cd" %>; border-left: 4px solid <%= 
                            "danger".equals(alertClass) ? "#dc3545" : 
                            "success".equals(alertClass) ? "#27ae60" : 
                            "#ffc107" %>; border-radius: 5px;">
                            <span style="font-size: 18px;"><%= icon %></span>
                            <div style="flex: 1;">
                                <div style="font-weight: 600; font-size: 13px; color: #333;">
                                    <span style="background: <%= 
                                        "danger".equals(alertClass) ? "#ffe0e0" : 
                                        "success".equals(alertClass) ? "#e0f7f0" : 
                                        "#fff0d9" %>; padding: 2px 8px; border-radius: 3px;">
                                        <%= eventType %>
                                    </span>
                                </div>
                                <div style="color: #666; font-size: 12px; margin-top: 4px;"><%= message %></div>
                                <div style="color: #999; font-size: 11px; margin-top: 4px;">
                                    <% 
                                        // Try to format timestamp nicely
                                        try {
                                            if (createdAt != null && createdAt.length() > 19) {
                                                createdAt = createdAt.substring(0, 19);
                                            }
                                        } catch (Exception e) {}
                                    %>
                                    <%= createdAt %>
                                </div>
                            </div>
                        </div>
                    <% } %>
                </div>
            <% } else { %>
                <div class="empty-state">
                    <div style="color: #999; font-size: 13px;">No events recorded yet. System events will appear here when nodes fail or recover.</div>
                </div>
            <% } %>
        </div>

        <!-- Instructions -->
        <div style="margin-top: 40px; padding: 20px; background: #f9f9f9; border-radius: 8px;">
            <h4 style="color: #333; margin-bottom: 10px;">💡 How Day 11 & 12 Work Together</h4>
            <ul style="color: #666; line-height: 1.8; margin-left: 20px;">
                <li><strong>Day 11 - Failure Detection:</strong> HeartbeatMonitor detects dead nodes (>15s timeout)</li>
                <li><strong>Day 12 - Metadata Purge:</strong> Deletes chunk_locations for dead node → triggers healing</li>
                <li><strong>Day 10 - Automatic Healing:</strong> ReplicationManager finds under-replicated chunks & copies them</li>
                <li><strong>Event Logging:</strong> Every failure & recovery logged to event_logs table (visible above)</li>
                <li><strong>Green 🟢:</strong> Node is healthy and ACTIVE</li>
                <li><strong>Red 🔴:</strong> Node is DEAD (will auto-recover below)</li>
                <li><strong>Recovery Progress:</strong> Shows healing percentage (chunks with RF ≥ 2)</li>
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
