<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="shared.User, dao.FileDAO, dao.NodeDAO, java.util.List, shared.FileMetadata, shared.NodeInfo, utils.NodeHealthUtil" %>
<%
    User user = (User) session.getAttribute("user");
    boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    if (user == null) {
        response.sendRedirect("login");
        return;
    }

    FileDAO fileDAO    = new FileDAO();
    NodeDAO nodeDAO    = new NodeDAO();
    List<FileMetadata> myFiles     = fileDAO.listFilesByOwner(user.getUserId());
    List<NodeInfo>     activeNodes = NodeHealthUtil.getReachableNodesSortedByCapacity(nodeDAO.getAllNodes());

    int  fileCount = (myFiles     != null) ? myFiles.size()     : 0;
    int  nodeCount = (activeNodes != null) ? activeNodes.size() : 0;
    boolean nodesAvailable = nodeCount > 0;

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
            color: inherit;
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
            font-size: 15px;
            transition: transform 0.2s, opacity 0.2s;
            display: block;
        }
        .action-btn:hover { transform: translateY(-2px); opacity: 0.92; }

        /* ── Recent Files table (Day 8) ───────────────────────────── */
        .files-card {
            background: white;
            padding: 24px 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            margin-bottom: 30px;
        }
        .files-card h3 { color: #333; margin-bottom: 16px; }
        .files-card table { width: 100%; border-collapse: collapse; font-size: 14px; }
        .files-card th {
            text-align: left;
            padding: 10px 14px;
            color: #888;
            font-size: 12px;
            text-transform: uppercase;
            letter-spacing: 0.04em;
            border-bottom: 2px solid #f0f0f0;
        }
        .files-card td {
            padding: 12px 14px;
            color: #555;
            border-bottom: 1px solid #f5f5f5;
            vertical-align: middle;
        }
        .files-card tr:last-child td { border-bottom: none; }
        .files-card tr:hover td { background: #fafaff; }
        .fname { font-weight: 600; color: #333; }
        .size-pill {
            display: inline-block;
            background: #f0f4ff;
            color: #667eea;
            border-radius: 12px;
            padding: 3px 10px;
            font-size: 12px;
            font-weight: 600;
        }
        .dl-btn {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 6px 14px;
            border-radius: 5px;
            font-size: 12px;
            font-weight: 600;
            text-decoration: none;
            transition: opacity 0.2s;
        }
        .dl-btn:hover { opacity: 0.85; }
        .view-all {
            display: block;
            text-align: center;
            margin-top: 16px;
            color: #667eea;
            font-size: 13px;
            font-weight: 600;
            text-decoration: none;
        }
        .view-all:hover { text-decoration: underline; }
        .empty-files { color: #aaa; font-size: 14px; text-align: center; padding: 20px 0; }

        /* ── Active Nodes ──────────────────────────────────────────── */
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
            flex-shrink: 0;
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

        <!-- Welcome Banner -->
        <div class="welcome-card">
            <h2>Welcome back, <%= user.getUsername() %>!</h2>
            <p>Distributed File Storage System &mdash; your files are split across multiple data nodes.</p>
            <% if (isAdmin) { %>
                <p>
                    <span class="status-dot <%= nodesAvailable ? "dot-green" : "dot-red" %>"></span>
                    System Status:
                    <%= nodesAvailable
                        ? nodeCount + " active data node(s) online"
                        : "No data nodes online — start run-datanode1.bat" %>
                </p>
            <% } else { %>
                <p>
                    <span class="status-dot <%= nodesAvailable ? "dot-green" : "dot-red" %>"></span>
                    <%= nodesAvailable
                        ? "Storage services are available."
                        : "Storage services are currently offline. You can view files, but upload/download/delete is unavailable." %>
                </p>
            <% } %>
        </div>

        <!-- Stats Grid -->
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
            <% if (isAdmin) { %>
                <div class="stat-card">
                    <div class="icon">&#128421;&#65039;</div>
                    <div class="number"><%= nodeCount %></div>
                    <div class="label">Active Nodes</div>
                </div>
            <% } else { %>
                <div class="stat-card">
                    <div class="icon"><%= nodesAvailable ? "&#9989;" : "&#9888;&#65039;" %></div>
                    <div class="number"><%= nodesAvailable ? "Online" : "Offline" %></div>
                    <div class="label">Storage Service</div>
                </div>
            <% } %>
            <a href="<%= request.getContextPath() %>/upload" class="stat-card"
               <%= !nodesAvailable ? "style='opacity:0.5; pointer-events:none; cursor:not-allowed;' title='Upload disabled: server offline'" : "" %>>
                <div class="icon">&#128228;</div>
                <div class="number">+</div>
                <div class="label">Upload New File</div>
            </a>
        </div>

        <!-- Quick Actions — Day 8: all three buttons are now ACTIVE -->
        <div class="actions-card">
            <h3>Quick Actions</h3>
            <div class="action-buttons">
                <a href="<%= request.getContextPath() %>/upload" class="action-btn"
                   <%= !nodesAvailable ? "style='opacity:0.5; pointer-events:none; cursor:not-allowed;' title='Upload disabled: server offline'" : "" %>>
                    &#128228; Upload File
                </a>
                <a href="<%= request.getContextPath() %>/files.jsp" class="action-btn">
                    &#128203; My Files
                </a>
            </div>
        </div>

        <!-- Recent Files with Download buttons — Day 8 -->
        <div class="files-card">
            <h3>&#128196; Recent Files</h3>
            <% if (myFiles == null || myFiles.isEmpty()) { %>
                <p class="empty-files">No files uploaded yet. Click "Upload File" to get started.</p>
            <% } else { %>
            <table>
                <thead>
                    <tr>
                        <th>File Name</th>
                        <th>Size</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                <%
                    int shown = 0;
                    for (FileMetadata f : myFiles) {
                        if (shown >= 5) break;
                        long sz = f.getFileSize();
                        String szStr;
                        if      (sz >= 1024L*1024*1024) szStr = String.format("%.2f GB", sz / (1024.0*1024*1024));
                        else if (sz >= 1024*1024)        szStr = String.format("%.1f MB", sz / (1024.0*1024));
                        else if (sz >= 1024)              szStr = String.format("%.1f KB", sz / 1024.0);
                        else                              szStr = sz + " B";
                %>
                    <tr>
                        <td><span class="fname">&#128196; <%= f.getFileName() %></span></td>
                        <td><span class="size-pill"><%= szStr %></span></td>
                        <td>
                                     <a href="<%= request.getContextPath() %>/download?file_id=<%= f.getFileId() %>"
                                         class="dl-btn"
                                         <%= !nodesAvailable ? "style='opacity:0.5; pointer-events:none; cursor:not-allowed;' title='Download disabled: no data nodes online'" : "" %>>&#128229; Download</a>
                        </td>
                    </tr>
                <%
                        shown++;
                    }
                %>
                </tbody>
            </table>
            <% if (myFiles.size() > 5) { %>
                <a href="<%= request.getContextPath() %>/files.jsp" class="view-all">
                    View all <%= myFiles.size() %> files &rarr;
                </a>
            <% } %>
            <% } %>
        </div>

        <!-- Active Data Nodes (Only for Admin) -->
        <% if (isAdmin && activeNodes != null && !activeNodes.isEmpty()) { %>
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
