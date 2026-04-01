<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="shared.User, dao.FileDAO, dao.ChunkDAO, java.util.List, shared.FileMetadata, shared.Chunk" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect("login");
        return;
    }

    FileDAO  fileDAO  = new FileDAO();
    ChunkDAO chunkDAO = new ChunkDAO();
    List<FileMetadata> myFiles = fileDAO.listFilesByOwner(user.getUserId());

    // Summary totals
    long totalBytes = 0;
    if (myFiles != null) for (FileMetadata f : myFiles) totalBytes += f.getFileSize();
    String totalStr;
    if      (totalBytes >= 1024L*1024*1024) totalStr = String.format("%.2f GB", totalBytes/(1024.0*1024*1024));
    else if (totalBytes >= 1024*1024)        totalStr = String.format("%.1f MB",  totalBytes/(1024.0*1024));
    else if (totalBytes >= 1024)              totalStr = String.format("%.1f KB",  totalBytes/1024.0);
    else                                      totalStr = totalBytes + " B";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Files - J-Cloud</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f5f7fa;
        }

        /* ── Navbar ──────────────────────────────────────────────── */
        .navbar {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 15px 30px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }
        .navbar h1 { font-size: 22px; }
        .navbar-links a {
            color: white;
            text-decoration: none;
            margin-left: 20px;
            font-weight: 500;
            opacity: 0.9;
        }
        .navbar-links a:hover { opacity: 1; text-decoration: underline; }

        /* ── Layout ──────────────────────────────────────────────── */
        .container { max-width: 1100px; margin: 36px auto; padding: 0 20px; }

        .page-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
        }
        .page-header h2 { color: #333; font-size: 20px; }
        .btn-new {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 10px 22px;
            border-radius: 8px;
            text-decoration: none;
            font-weight: 600;
            font-size: 14px;
            transition: opacity 0.2s;
        }
        .btn-new:hover { opacity: 0.88; }

        /* ── Summary bar ─────────────────────────────────────────── */
        .summary-bar {
            background: #f0f4ff;
            border: 1px solid #d4dcf7;
            border-radius: 8px;
            padding: 12px 20px;
            margin-bottom: 20px;
            font-size: 14px;
            color: #555;
            display: flex;
            gap: 28px;
        }
        .summary-bar strong { color: #667eea; }

        /* ── Card / Table ────────────────────────────────────────── */
        .card {
            background: white;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            overflow: hidden;
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }
        thead {
            background: #f4f5ff;
        }
        thead th {
            padding: 14px 18px;
            text-align: left;
            font-size: 12px;
            color: #777;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        tbody tr {
            border-bottom: 1px solid #f0f0f0;
            transition: background 0.12s;
        }
        tbody tr:last-child { border-bottom: none; }
        tbody tr:hover { background: #fafaff; }
        td {
            padding: 14px 18px;
            font-size: 14px;
            color: #444;
            vertical-align: middle;
        }

        /* File name cell */
        .file-name-cell {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .file-icon { font-size: 20px; line-height: 1; }
        .file-name-text {
            font-weight: 600;
            color: #2d2d2d;
            word-break: break-all;
        }

        /* Badges */
        .size-badge {
            display: inline-block;
            background: #eef1ff;
            color: #667eea;
            border-radius: 12px;
            padding: 4px 12px;
            font-size: 12px;
            font-weight: 700;
            white-space: nowrap;
        }
        .chunk-badge {
            display: inline-block;
            background: #fff4e6;
            color: #d4640a;
            border-radius: 12px;
            padding: 4px 12px;
            font-size: 12px;
            font-weight: 700;
            white-space: nowrap;
        }

        /* Download button */
        .download-btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 8px 18px;
            border-radius: 6px;
            text-decoration: none;
            font-size: 13px;
            font-weight: 600;
            white-space: nowrap;
            transition: opacity 0.2s, transform 0.15s;
        }
        .download-btn:hover { opacity: 0.88; transform: translateY(-1px); }

        .delete-btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: #ffe8e8;
            color: #b03030;
            border: 1px solid #f3b4b4;
            padding: 8px 14px;
            border-radius: 6px;
            font-size: 13px;
            font-weight: 700;
            cursor: pointer;
            white-space: nowrap;
            margin-left: 8px;
            transition: background 0.2s;
        }
        .delete-btn:hover { background: #ffdcdc; }

        .action-group {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
        }

        .inline-form {
            display: inline;
            margin: 0;
        }

        /* Empty state */
        .empty-state {
            text-align: center;
            padding: 64px 20px;
            color: #999;
        }
        .empty-state .empty-icon { font-size: 56px; margin-bottom: 16px; }
        .empty-state p { font-size: 15px; margin-bottom: 22px; }
    </style>
</head>
<body>

    <div class="navbar">
        <h1>&#9729;&#65039; J-Cloud</h1>
        <div class="navbar-links">
            <a href="<%= request.getContextPath() %>/dashboard.jsp">Dashboard</a>
            <a href="<%= request.getContextPath() %>/upload">Upload</a>
            <a href="<%= request.getContextPath() %>/logout">Logout</a>
        </div>
    </div>

    <div class="container">

        <!-- Page header -->
        <div class="page-header">
            <h2>&#128203; My Files</h2>
            <a href="<%= request.getContextPath() %>/upload" class="btn-new">&#128228; Upload New</a>
        </div>

        <!-- Summary bar — only shown when files exist -->
        <% if (myFiles != null && !myFiles.isEmpty()) { %>
        <div class="summary-bar">
            Total files: <strong><%= myFiles.size() %></strong>
            &nbsp;&nbsp;|&nbsp;&nbsp;
            Total size: <strong><%= totalStr %></strong>
        </div>
        <% } %>

        <!-- File table or empty state -->
        <div class="card">
            <% if (myFiles == null || myFiles.isEmpty()) { %>

            <div class="empty-state">
                <div class="empty-icon">&#128193;</div>
                <p>You haven't uploaded any files yet.</p>
                <a href="<%= request.getContextPath() %>/upload" class="download-btn">
                    &#128228; Upload your first file
                </a>
            </div>

            <% } else { %>

            <table>
                <thead>
                    <tr>
                        <th style="width:45%">File Name</th>
                        <th style="width:15%">Size</th>
                        <th style="width:15%">Chunks</th>
                        <th style="width:25%">Action</th>
                    </tr>
                </thead>
                <tbody>
                <%
                    for (FileMetadata file : myFiles) {

                        // Chunk count
                        List<Chunk> chunks = chunkDAO.getChunksByFileId(file.getFileId());
                        int chunkCount = (chunks != null) ? chunks.size() : 0;

                        // Human-readable size
                        long sz = file.getFileSize();
                        String sizeStr;
                        if      (sz >= 1024L*1024*1024) sizeStr = String.format("%.2f GB", sz/(1024.0*1024*1024));
                        else if (sz >= 1024*1024)        sizeStr = String.format("%.1f MB",  sz/(1024.0*1024));
                        else if (sz >= 1024)              sizeStr = String.format("%.1f KB",  sz/1024.0);
                        else                              sizeStr = sz + " B";

                        // Icon by extension
                        String fname = file.getFileName().toLowerCase();
                        String icon;
                        if      (fname.endsWith(".pdf"))                                                     icon = "&#128209;";
                        else if (fname.endsWith(".mp4")||fname.endsWith(".mkv")||
                                 fname.endsWith(".avi")||fname.endsWith(".mov"))                              icon = "&#127916;";
                        else if (fname.endsWith(".mp3")||fname.endsWith(".wav")||fname.endsWith(".flac"))     icon = "&#127925;";
                        else if (fname.endsWith(".jpg")||fname.endsWith(".jpeg")||
                                 fname.endsWith(".png")||fname.endsWith(".gif")||fname.endsWith(".webp"))     icon = "&#128444;&#65039;";
                        else if (fname.endsWith(".zip")||fname.endsWith(".rar")||
                                 fname.endsWith(".tar")||fname.endsWith(".gz"))                               icon = "&#128230;";
                        else if (fname.endsWith(".txt")||fname.endsWith(".csv")||fname.endsWith(".md"))       icon = "&#128203;";
                        else if (fname.endsWith(".java")||fname.endsWith(".py")||
                                 fname.endsWith(".js")||fname.endsWith(".html"))                              icon = "&#128187;";
                        else                                                                                  icon = "&#128196;";
                %>
                <tr>
                    <td>
                        <div class="file-name-cell">
                            <span class="file-icon"><%= icon %></span>
                            <span class="file-name-text"><%= file.getFileName() %></span>
                        </div>
                    </td>
                    <td><span class="size-badge"><%= sizeStr %></span></td>
                    <td>
                        <span class="chunk-badge">
                            <%= chunkCount %> chunk<%= chunkCount == 1 ? "" : "s" %>
                        </span>
                    </td>
                    <td>
                        <div class="action-group">
                            <a href="<%= request.getContextPath() %>/download?file_id=<%= file.getFileId() %>"
                               class="download-btn">
                                &#128229; Download
                            </a>
                            <form method="post"
                                  action="<%= request.getContextPath() %>/delete"
                                  class="inline-form"
                                  onsubmit="return confirm('Delete this file? The file will disappear immediately while chunk cleanup runs in background.');">
                                <input type="hidden" name="file_id" value="<%= file.getFileId() %>">
                                <button type="submit" class="delete-btn">&#128465;&#65039; Delete</button>
                            </form>
                        </div>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>

            <% } %>
        </div><!-- /card -->

    </div><!-- /container -->
</body>
</html>
