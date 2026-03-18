<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="shared.User, dao.FileDAO, dao.NodeDAO, java.util.List, shared.FileMetadata, shared.NodeInfo" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect("login");
        return;
    }

    FileDAO fileDAO    = new FileDAO();
    NodeDAO nodeDAO    = new NodeDAO();
    List<FileMetadata> myFiles     = fileDAO.listFilesByOwner(user.getUserId());
    List<NodeInfo>     activeNodes = nodeDAO.getAllActiveNodes();

    int  fileCount = (myFiles     != null) ? myFiles.size()     : 0;
    int  nodeCount = (activeNodes != null) ? activeNodes.size() : 0;

    long totalBytes = 0;
    if (myFiles != null) {
        for (FileMetadata f : myFiles) totalBytes += f.getFileSize();
    }
    String storageUsed;
    if (totalBytes >= 1024L * 1024 * 1024) {
        storageUsed = String.format("%.2f GB", totalBytes / (1024.0 * 1024 * 1024));
    } else if (totalBytes >= 1024 * 1024) {
        storageUsed = String.format("%.1f MB", totalBytes / (1024.0 * 1024));
    } else {
        storageUsed = totalBytes + " B";
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - J-Cloud</title>
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
        .user-info { display: flex; align-items: center; gap: 20px; }
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
        .welcome-card {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            margin-bottom: 30px;
        }
        .welcome-card h2 { color: #667eea; margin-bottom: 10px; }
        .welcome-card p  { color: #666; margin-bottom: 6px; }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .stat-card {
            background: white;
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            text-align: center;
            text-decoration: none;
            display: block;
            transition: transform 0.15s, box-shadow 0.15s;
        }
        .stat-card:hover { transform: translateY(-3px); box-shadow: 0 6px 16px rgba(0,0,0,0.12); }
        .stat-card .icon   { font-size: 48px; margin-bottom: 10px; }
        .stat-card .number { font-size: 32px; font-weight: bold; color: #667eea; margin-bottom: 5px; }
        .stat-card .label  { color: #666; font-size: 14px; }
        .actions-card {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            margin-bottom: 30px;
        }
        .actions-card h3 { color: #333; margin-bottom: 20px; }
        .action-buttons {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
        }
        .action-btn {
            padding: 15px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-align: center;
            border-radius: 8px;
            text-decoration: none;
            font-weight: 500;
            transition: transform 0.2s, opacity 0.2s;
            display: block;
        }
        .action-btn:hover { transform: translateY(-2px); opacity: 0.92; }
        .action-btn.disabled {
            background: #ccc;
            cursor: not-allowed;
            pointer-events: none;
        }
        .nodes-card {
            background: white;
            padding: 24px 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .nodes-card h3 { color: #333; margin-bottom: 16px; }
        .node-row {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 10px 0;
            border-bottom: 1px solid #f0f0f0;
            font-size: 14px;
            color: #555;
        }
        .node-row:last-child { border-bottom: none; }
        .status-dot {
            display: inline-block;
            width: 10px; height: 10px;
            border-radius: 50%;
        }
        .dot-green { background: #4caf50; }
        .dot-red   { background: #f44336; }
        .node-badge {
            display: inline-block;
            padding: 3px 10px;
            border-radius: 10px;
            font-size: 12px;
            font-weight: 600;
            background: #e6ffed;
            color: #1f5f2d;
        }
        .coming-soon {
            font-size: 11px;
            color: #aaa;
            display: block;
            margin-top: 4px;
        }
    </style>
</head>
<body>
    <div class="navbar">
        <h1>&#9729;&#65039; J-Cloud Dashboard</h1>
        <div class="user-info">
            <span>Welcome, <strong><%= user.getUsername() %></strong></span>
            <a href="<%= request.getContextPath() %>/logout" class="logout-btn">Logout</a>
        </div>
    </div>

    <div class="container">
        <div class="welcome-card">
            <h2>Welcome back, <%= user.getUsername() %>!</h2>
            <p>Distributed File Storage System &mdash; your files are split across multiple data nodes.</p>
            <p>
                <span class="status-dot <%= nodeCount > 0 ? "dot-green" : "dot-red" %>"></span>
                System Status:
                <%= nodeCount > 0
                    ? nodeCount + " active data node(s) online"
                    : "No data nodes online — start run-datanode1.bat" %>
            </p>
        </div>

        <div class="stats-grid">
            <div class="stat-card">
                <div class="icon">&#128193;</div>
                <div class="number"><%= fileCount %></div>
                <div class="label">Files Uploaded</div>
            </div>
            <div class="stat-card">
                <div class="icon">&#128190;</div>
                <div class="number"><%= storageUsed %></div>
                <div class="label">Storage Used</div>
            </div>
            <div class="stat-card">
                <div class="icon">&#128421;&#65039;</div>
                <div class="number"><%= nodeCount %></div>
                <div class="label">Active Nodes</div>
            </div>
            <a href="<%= request.getContextPath() %>/upload" class="stat-card">
                <div class="icon">&#128228;</div>
                <div class="number">+</div>
                <div class="label">Upload New File</div>
            </a>
        </div>

        <div class="actions-card">
            <h3>Quick Actions</h3>
            <div class="action-buttons">
                <a href="<%= request.getContextPath() %>/upload" class="action-btn">
                    &#128228; Upload File
                </a>
                <a href="#" class="action-btn disabled">
                    &#128229; Download File
                    <span class="coming-soon">(Day 6 — coming soon)</span>
                </a>
                <a href="#" class="action-btn disabled">
                    &#128203; My Files
                    <span class="coming-soon">(Day 6 — coming soon)</span>
                </a>
            </div>
        </div>

        <% if (activeNodes != null && !activeNodes.isEmpty()) { %>
        <div class="nodes-card">
            <h3>&#128421;&#65039; Active Data Nodes</h3>
            <% for (NodeInfo n : activeNodes) { %>
            <div class="node-row">
                <span class="status-dot dot-green"></span>
                <strong><%= n.getNodeName() %></strong>
                &mdash;
                <%= n.getIpAddress() %>:<%= n.getPort() %>
                <span class="node-badge"><%= n.getStatus() %></span>
            </div>
            <% } %>
        </div>
        <% } %>
    </div>
</body>
</html>