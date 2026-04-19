<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="shared.User, dao.FileDAO, dao.ChunkDAO, dao.NodeDAO, java.util.List, shared.FileMetadata, shared.Chunk, shared.NodeInfo, utils.NodeHealthUtil" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) { response.sendRedirect("login"); return; }

    FileDAO  fileDAO  = new FileDAO();
    ChunkDAO chunkDAO = new ChunkDAO();
    NodeDAO  nodeDAO  = new NodeDAO();
    List<FileMetadata> myFiles = fileDAO.listFilesByOwner(user.getUserId());
    List<NodeInfo> reachableNodes = NodeHealthUtil.getReachableNodesSortedByCapacity(nodeDAO.getAllNodes());
    boolean nodesAvailable = !reachableNodes.isEmpty();

    long totalBytes = 0;
    if (myFiles != null) for (FileMetadata f : myFiles) totalBytes += f.getFileSize();
    String totalStr;
    if      (totalBytes >= 1024L*1024*1024) totalStr = String.format("%.2f GB", totalBytes/(1024.0*1024*1024));
    else if (totalBytes >= 1024*1024)        totalStr = String.format("%.1f MB",  totalBytes/(1024.0*1024));
    else if (totalBytes >= 1024)             totalStr = String.format("%.1f KB",  totalBytes/1024.0);
    else                                     totalStr = totalBytes + " B";

    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Files - J-Cloud</title>
    <style src="https://cdnjs.cloudflare.com/ajax/libs/mammoth/1.6.0/mammoth.browser.min.js"></script>
    <style>
        * { margin:0; padding:0; box-sizing:border-box; }
        body { font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif; background:#f5f7fa; }

        .navbar {
            background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);
            color:white; padding:15px 30px;
            display:flex; justify-content:space-between; align-items:center;
            box-shadow:0 2px 5px rgba(0,0,0,.1);
        }
        .navbar h1 { font-size:22px; }
        .navbar-links a { color:white; text-decoration:none; margin-left:20px; font-weight:500; opacity:.9; }
        .navbar-links a:hover { opacity:1; text-decoration:underline; }

        .container { max-width:1100px; margin:36px auto; padding:0 20px; }

        .page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
        .page-header h2 { color:#333; font-size:20px; }
        .btn-new {
            background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);
            color:white; padding:10px 22px; border-radius:8px;
            text-decoration:none; font-weight:600; font-size:14px; transition:opacity .2s;
        }
        .btn-new:hover { opacity:.88; }

        .summary-bar {
            background:#f0f4ff; border:1px solid #d4dcf7; border-radius:8px;
            padding:12px 20px; margin-bottom:20px; font-size:14px; color:#555;
            display:flex; gap:28px;
        }
        .summary-bar strong { color:#667eea; }

        .node-alert {
            background:#ffe7e7; border:1px solid #f3a6a6; border-radius:8px;
            padding:14px 18px; margin-bottom:20px; color:#c00; font-size:14px;
        }

        .card { background:white; border-radius:10px; box-shadow:0 2px 10px rgba(0,0,0,.08); overflow:hidden; }
        table { width:100%; border-collapse:collapse; }
        thead { background:#f4f5ff; }
        thead th { padding:14px 18px; text-align:left; font-size:12px; color:#777; font-weight:700; text-transform:uppercase; letter-spacing:.05em; }
        tbody tr { border-bottom:1px solid #f0f0f0; transition:background .12s; }
        tbody tr:last-child { border-bottom:none; }
        tbody tr:hover { background:#fafaff; }
        td { padding:14px 18px; font-size:14px; color:#444; vertical-align:middle; }

        .file-name-cell { display:flex; align-items:center; gap:10px; cursor:pointer; }
        .file-icon { font-size:20px; line-height:1; }
        .file-name-text { font-weight:600; color:#2d2d2d; word-break:break-all; }
        .file-name-text:hover { color:#667eea; text-decoration:underline; }

        .size-badge { display:inline-block; background:#eef1ff; color:#667eea; border-radius:12px; padding:4px 12px; font-size:12px; font-weight:700; white-space:nowrap; }
        .chunk-badge { display:inline-block; background:#fff4e6; color:#d4640a; border-radius:12px; padding:4px 12px; font-size:12px; font-weight:700; white-space:nowrap; }

        .action-group { display:flex; align-items:center; flex-wrap:wrap; gap:6px; }
        .preview-btn {
            display:inline-flex; align-items:center; gap:5px;
            background:#f0f4ff; color:#667eea; border:1px solid #d4dcf7;
            padding:7px 14px; border-radius:6px; font-size:12px; font-weight:600;
            cursor:pointer; transition:background .15s; white-space:nowrap;
        }
        .preview-btn:hover { background:#e4e9ff; }
        .preview-btn.disabled {
            opacity: 0.5;
            cursor: not-allowed;
            pointer-events: none;
            display: inline-flex;
            align-items: center;
            gap: 5px;
            background: #f0f4ff;
            color: #667eea;
            border: 1px solid #d4dcf7;
            padding: 7px 14px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
            white-space: nowrap;
        }
        .download-btn {
            display:inline-flex; align-items:center; gap:5px;
            background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);
            color:white; padding:7px 14px; border-radius:6px;
            text-decoration:none; font-size:12px; font-weight:600;
            white-space:nowrap; transition:opacity .2s;
        }
        .download-btn:hover { opacity:.88; }
        .delete-btn {
            display:inline-flex; align-items:center; gap:5px;
            background:#ffe8e8; color:#b03030; border:1px solid #f3b4b4;
            padding:7px 12px; border-radius:6px; font-size:12px; font-weight:700;
            cursor:pointer; transition:background .2s;
        }
        .delete-btn:hover { background:#ffdcdc; }
        
        /* Disabled button styles */
        .download-btn.disabled, .delete-btn.disabled {
            opacity: 0.5;
            cursor: not-allowed;
            pointer-events: none;
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 7px 14px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
            white-space: nowrap;
        }
        .download-btn.disabled {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }
        .delete-btn.disabled {
            background: #ffe8e8;
            color: #b03030;
            border: 1px solid #f3b4b4;
        }
        
        .inline-form { display:inline; margin:0; }

        .empty-state { text-align:center; padding:64px 20px; color:#999; }
        .empty-state .ei { font-size:56px; margin-bottom:16px; }
        .empty-state p { font-size:15px; margin-bottom:22px; }

        /* -- Preview Drawer -- */
        .drawer-overlay {
            display:none; position:fixed; inset:0;
            background:rgba(0,0,0,.45); z-index:900;
        }
        .drawer-overlay.open { display:block; }

        .drawer {
            position:fixed; top:0; right:-640px; width:620px; max-width:95vw;
            height:100vh; background:white;
            box-shadow:-4px 0 32px rgba(0,0,0,.15);
            display:flex; flex-direction:column;
            transition:right .28s cubic-bezier(.4,0,.2,1);
            z-index:901;
        }
        .drawer.open { right:0; }

        .drawer-header {
            display:flex; align-items:center; gap:12px;
            padding:16px 20px; border-bottom:1px solid #f0f0f0;
            flex-shrink:0;
        }
        .drawer-icon { font-size:22px; line-height:1; }
        .drawer-title { font-size:15px; font-weight:600; color:#222; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
        .drawer-meta { font-size:12px; color:#888; flex-shrink:0; }
        .drawer-close {
            width:32px; height:32px; border-radius:50%;
            border:none; background:#f0f0f0; font-size:16px;
            cursor:pointer; display:flex; align-items:center; justify-content:center;
            flex-shrink:0; transition:background .15s;
        }
        .drawer-close:hover { background:#e0e0e0; }

        .drawer-toolbar {
            display:flex; gap:10px; padding:12px 20px;
            border-bottom:1px solid #f5f5f5; flex-shrink:0;
        }
        .drawer-dl-btn {
            display:inline-flex; align-items:center; gap:6px;
            background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);
            color:white; padding:8px 18px; border-radius:7px;
            text-decoration:none; font-size:13px; font-weight:600;
            transition:opacity .2s;
        }
        .drawer-dl-btn:hover { opacity:.88; }
        .drawer-new-tab-btn {
            display:inline-flex; align-items:center; gap:6px;
            background:#f0f4ff; color:#667eea; border:1px solid #d4dcf7;
            padding:8px 18px; border-radius:7px; font-size:13px; font-weight:600;
            cursor:pointer; text-decoration:none; transition:background .15s;
        }
        .drawer-new-tab-btn:hover { background:#e4e9ff; }

        .drawer-body {
            flex:1; overflow:auto; display:flex;
            align-items:flex-start; justify-content:center;
            padding:20px; background:#fafafa;
        }

        /* Preview renderers */
        .preview-img { max-width:100%; max-height:calc(100vh - 200px); object-fit:contain; border-radius:6px; box-shadow:0 2px 12px rgba(0,0,0,.12); }
        .preview-pdf { width:100%; height:calc(100vh - 200px); border:none; border-radius:6px; }
        .preview-video { max-width:100%; max-height:calc(100vh - 200px); border-radius:6px; }
        .preview-audio { width:100%; margin-top:60px; }
        .preview-text {
            width:100%; min-height:300px; background:white;
            border:1px solid #e8e8e8; border-radius:8px;
            padding:20px 24px; font-family:'Courier New',monospace; font-size:13px;
            line-height:1.7; color:#333; white-space:pre-wrap; word-break:break-all;
            overflow-y:auto; text-align:left;
        }
        .preview-unsupported {
            text-align:center; padding:60px 20px; color:#999;
        }
        .preview-unsupported .ui { font-size:52px; margin-bottom:16px; }
        .preview-unsupported p { font-size:15px; margin-bottom:20px; }

        .preview-loading {
            display:flex; flex-direction:column; align-items:center;
            justify-content:center; gap:16px; padding:60px 20px; color:#888;
        }
        .spinner {
            width:36px; height:36px; border:3px solid #e0e0e0;
            border-top-color:#667eea; border-radius:50%;
            animation:spin .7s linear infinite;
        }
        @keyframes spin { to { transform:rotate(360deg); } }
    </style>
</head>
<body>

<div class="navbar">
    <h1>&#9729;&#65039; J-Cloud</h1>
    <div class="navbar-links">
        <a href="<%= ctx %>/dashboard.jsp">Dashboard</a>
        <a href="<%= ctx %>/upload">Upload</a>
        <a href="<%= ctx %>/logout">Logout</a>
    </div>
</div>

<div class="container">

    <div class="page-header">
        <h2>&#128203; My Files</h2>
        <a href="<%= ctx %>/upload" class="btn-new">&#128228; Upload New</a>
    </div>

    <% if (myFiles != null && !myFiles.isEmpty()) { %>
    <div class="summary-bar">
        Total files: <strong><%= myFiles.size() %></strong>
        &nbsp;|&nbsp;
        Total size: <strong><%= totalStr %></strong>
    </div>
    <% } %>

    <% if (!nodesAvailable) { %>
    <div class="node-alert">
        <strong>&#9888; System Offline:</strong> All data nodes are currently offline. 
        Download, delete, and preview are disabled until at least one data node comes online.
    </div>
    <% } %>

    <div class="card">
    <% if (myFiles == null || myFiles.isEmpty()) { %>
        <div class="empty-state">
            <div class="ei">&#128193;</div>
            <p>You haven't uploaded any files yet.</p>
            <a href="<%= ctx %>/upload" class="download-btn">&#128228; Upload your first file</a>
        </div>
    <% } else { %>
        <table>
            <thead><tr>
                <th style="width:42%">File Name</th>
                <th style="width:13%">Size</th>
                <th style="width:13%">Chunks</th>
                <th>Actions</th>
            </tr></thead>
            <tbody>
            <% for (FileMetadata file : myFiles) {
                List<Chunk> chunks = chunkDAO.getChunksByFileId(file.getFileId());
                int chunkCount = (chunks != null) ? chunks.size() : 0;
                long sz = file.getFileSize();
                String sizeStr;
                if      (sz >= 1024L*1024*1024) sizeStr = String.format("%.2f GB", sz/(1024.0*1024*1024));
                else if (sz >= 1024*1024)        sizeStr = String.format("%.1f MB", sz/(1024.0*1024));
                else if (sz >= 1024)             sizeStr = String.format("%.1f KB", sz/1024.0);
                else                             sizeStr = sz + " B";

                String fname = file.getFileName().toLowerCase();
                String icon;
                if      (fname.endsWith(".pdf"))                                                icon = "&#128209;";
                else if (fname.endsWith(".mp4")||fname.endsWith(".mkv")||fname.endsWith(".avi")||fname.endsWith(".mov")) icon = "&#127916;";
                else if (fname.endsWith(".mp3")||fname.endsWith(".wav")||fname.endsWith(".flac")) icon = "&#127925;";
                else if (fname.endsWith(".jpg")||fname.endsWith(".jpeg")||fname.endsWith(".png")||fname.endsWith(".gif")||fname.endsWith(".webp")) icon = "&#128444;&#65039;";
                else if (fname.endsWith(".zip")||fname.endsWith(".rar")||fname.endsWith(".tar")||fname.endsWith(".gz")) icon = "&#128230;";
                else if (fname.endsWith(".txt")||fname.endsWith(".csv")||fname.endsWith(".md")) icon = "&#128203;";
                else if (fname.endsWith(".java")||fname.endsWith(".py")||fname.endsWith(".js")||fname.endsWith(".html")) icon = "&#128187;";
                else icon = "&#128196;";

                // Determine if this file type supports preview
                String previewType = "none";
                if (fname.endsWith(".jpg")||fname.endsWith(".jpeg")||fname.endsWith(".png")||fname.endsWith(".gif")||fname.endsWith(".webp")||fname.endsWith(".svg")||fname.endsWith(".bmp")) previewType = "image";
                else if (fname.endsWith(".pdf")) previewType = "pdf";
                else if (fname.endsWith(".mp4")||fname.endsWith(".webm")||fname.endsWith(".ogv")) previewType = "video";
                else if (fname.endsWith(".mp3")||fname.endsWith(".wav")||fname.endsWith(".ogg")||fname.endsWith(".flac")) previewType = "audio";
                else if (fname.endsWith(".txt")||fname.endsWith(".md")||fname.endsWith(".csv")||fname.endsWith(".json")||fname.endsWith(".xml")||fname.endsWith(".java")||fname.endsWith(".py")||fname.endsWith(".js")||fname.endsWith(".ts")||fname.endsWith(".css")||fname.endsWith(".html")||fname.endsWith(".sh")||fname.endsWith(".sql")||fname.endsWith(".yaml")||fname.endsWith(".yml")||fname.endsWith(".toml")||fname.endsWith(".ini")||fname.endsWith(".c")||fname.endsWith(".cpp")||fname.endsWith(".h")) previewType = "text";
            %>
            <tr>
                <td>
                    <div class="file-name-cell"
                         onclick="openPreview(<%= file.getFileId() %>,'<%= file.getFileName().replace("'","\\'").replace("\"","&quot;") %>','<%= sizeStr %>','<%= previewType %>','<%= icon %>')">
                        <span class="file-icon"><%= icon %></span>
                        <span class="file-name-text"><%= file.getFileName() %></span>
                    </div>
                </td>
                <td><span class="size-badge"><%= sizeStr %></span></td>
                <td><span class="chunk-badge"><%= chunkCount %> chunk<%= chunkCount==1?"":"s" %></span></td>
                <td>
                    <div class="action-group">
                        <% if (!"none".equals(previewType)) { %>
                            <% if (nodesAvailable) { %>
                            <button class="preview-btn"
                                onclick="openPreview(<%= file.getFileId() %>,'<%= file.getFileName().replace("'","\\'").replace("\"","&quot;") %>','<%= sizeStr %>','<%= previewType %>','<%= icon %>')">
                                &#128065;&#65039; Preview
                            </button>
                            <% } else { %>
                            <span class="preview-btn disabled" title="Preview disabled: no data nodes online">&#128065;&#65039; Preview</span>
                            <% } %>
                        <% } %>
                        <% if (nodesAvailable) { %>
                        <a href="<%= ctx %>/download?file_id=<%= file.getFileId() %>" class="download-btn">
                            &#128229; Download
                        </a>
                        <form method="post" action="<%= ctx %>/delete" class="inline-form"
                              onsubmit="return confirm('Delete this file permanently?');">
                            <input type="hidden" name="file_id" value="<%= file.getFileId() %>">
                            <button type="submit" class="delete-btn">&#128465;&#65039;</button>
                        </form>
                        <% } else { %>
                        <span class="download-btn disabled" title="Download disabled: no data nodes online">&#128229; Download</span>
                        <span class="delete-btn disabled" title="Delete disabled: no data nodes online">&#128465;&#65039;</span>
                        <% } %>
                    </div>
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
    <% } %>
    </div>
</div>

<!-- -- Preview Drawer -- -->
<div class="drawer-overlay" id="drawerOverlay" onclick="closeDrawer()"></div>

<div class="drawer" id="drawer">
    <div class="drawer-header">
        <span class="drawer-icon" id="drawerIcon">&#128196;</span>
        <span class="drawer-title" id="drawerTitle">File name</span>
        <span class="drawer-meta" id="drawerMeta"></span>
        <button class="drawer-close" onclick="closeDrawer()">&#10005;</button>
    </div>
    <div class="drawer-toolbar">
        <a id="drawerDownload" href="#" class="drawer-dl-btn">&#128229; Download</a>
        <a id="drawerNewTab"   href="#" target="_blank" class="drawer-new-tab-btn">&#128279; Open in new tab</a>
    </div>
    <div class="drawer-body" id="drawerBody">
        <div class="preview-loading"><div class="spinner"></div><span>Loading preview�</span></div>
    </div>
</div>

<script>
const CTX = '<%= ctx %>';
let currentFileId = null;

function openPreview(fileId, fileName, fileSize, previewType, icon) {
    currentFileId = fileId;
    document.getElementById('drawerIcon').innerHTML    = icon;
    document.getElementById('drawerTitle').textContent = fileName;
    document.getElementById('drawerMeta').textContent  = fileSize;
    document.getElementById('drawerDownload').href     = CTX + '/download?file_id=' + fileId;
    document.getElementById('drawerNewTab').href       = CTX + '/preview?file_id='  + fileId;
    document.getElementById('drawerBody').innerHTML    =
        '<div class="preview-loading"><div class="spinner"></div><span>Loading preview\u2026</span></div>';

    document.getElementById('drawerOverlay').classList.add('open');
    document.getElementById('drawer').classList.add('open');
    document.body.style.overflow = 'hidden';

    const url = CTX + '/preview?file_id=' + fileId;

    if (previewType === 'image') {
        const img = new Image();
        img.className = 'preview-img';
        img.onload = () => { document.getElementById('drawerBody').innerHTML = ''; document.getElementById('drawerBody').appendChild(img); };
        img.onerror = () => showUnsupported(fileName);
        img.src = url;

    } else if (previewType === 'pdf') {
        document.getElementById('drawerBody').innerHTML =
            '<iframe class="preview-pdf" src="' + url + '" title="PDF preview"></iframe>';

    } else if (previewType === 'video') {
        document.getElementById('drawerBody').innerHTML =
            '<video class="preview-video" controls><source src="' + url + '">Your browser does not support video.</video>';

    } else if (previewType === 'audio') {
        document.getElementById('drawerBody').innerHTML =
            '<audio class="preview-audio" controls><source src="' + url + '">Your browser does not support audio.</audio>';

    } else if (previewType === 'text') {
        fetch(url)
            .then(r => {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.text();
            })
            .then(text => {
                const pre = document.createElement('pre');
                pre.className = 'preview-text';
                // Show max 500 lines to avoid freezing on huge files
                const lines = text.split('\n');
                const truncated = lines.length > 500;
                pre.textContent = truncated
                    ? lines.slice(0, 500).join('\n') + '\n\n� (showing first 500 lines � download to see full file)'
                    : text;
                document.getElementById('drawerBody').innerHTML = '';
                document.getElementById('drawerBody').appendChild(pre);
            })
            .catch(() => showUnsupported(fileName));

    } else {
        showUnsupported(fileName);
    }
}

function showUnsupported(fileName) {
    document.getElementById('drawerBody').innerHTML =
        '<div class="preview-unsupported">' +
        '<div class="ui">&#128230;</div>' +
        '<p>Preview not available for this file type.</p>' +
        '<a href="' + CTX + '/download?file_id=' + currentFileId + '" class="download-btn">&#128229; Download to view</a>' +
        '</div>';
}

function closeDrawer() {
    document.getElementById('drawerOverlay').classList.remove('open');
    document.getElementById('drawer').classList.remove('open');
    document.body.style.overflow = '';
    // Stop media playback when closing
    document.getElementById('drawerBody').innerHTML = '';
    currentFileId = null;
}

document.addEventListener('keydown', e => { if (e.key === 'Escape') closeDrawer(); });
</script>
</body>
</html>
