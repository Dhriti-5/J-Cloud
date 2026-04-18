<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="shared.User" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect("login");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Upload Files - J-Cloud</title>
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
        .navbar a {
            color: white;
            text-decoration: none;
            margin-left: 15px;
            font-weight: 500;
        }
        .navbar a:hover { opacity: 0.8; }
        .container { max-width: 800px; margin: 40px auto; padding: 20px; }
        .card {
            background: white;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            padding: 30px;
        }
        .card h1 { color: #333; margin-bottom: 8px; font-size: 22px; }
        .card .subtitle { color: #666; margin-bottom: 24px; font-size: 14px; }

        /* ── Messages ─────────────────────────────────────────────────── */
        .message {
            padding: 14px 18px;
            border-radius: 8px;
            margin-bottom: 20px;
            font-size: 14px;
        }
        .message.success {
            background: #e6ffed;
            border: 1px solid #8ee5a2;
            color: #1f5f2d;
        }
        .message.error {
            background: #ffe6e6;
            border: 1px solid #f29b9b;
            color: #7d1f1f;
        }

        /* ── Limit badge row ──────────────────────────────────────────── */
        .limit-row {
            display: flex;
            gap: 10px;
            margin-bottom: 16px;
        }
        .limit-badge {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            background: #f0f4ff;
            border: 1px solid #d4dcf7;
            color: #667eea;
            font-size: 12px;
            font-weight: 700;
            padding: 5px 12px;
            border-radius: 20px;
        }

        /* ── Drop zone ────────────────────────────────────────────────── */
        .drop-zone {
            border: 2px dashed #c4c4e0;
            border-radius: 10px;
            padding: 44px 20px;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s;
            margin-bottom: 16px;
            background: #fafaff;
        }
        .drop-zone.drag-over {
            border-color: #667eea;
            background: #f0f0ff;
        }
        .drop-zone.has-error {
            border-color: #e74c3c;
            background: #fff8f8;
        }
        .drop-zone .icon { font-size: 42px; margin-bottom: 12px; }
        .drop-zone p { color: #666; font-size: 15px; }
        .drop-zone .hint { color: #aaa; font-size: 12px; margin-top: 6px; }
        .drop-zone span { color: #667eea; font-weight: 600; }

        /* ── File list ────────────────────────────────────────────────── */
        .file-list {
            display: none;
            background: #f8f9ff;
            border: 1px solid #e0e4ff;
            border-radius: 8px;
            padding: 12px 16px;
            margin-bottom: 16px;
            max-height: 220px;
            overflow-y: auto;
        }
        .file-list-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
        }
        .file-list-title {
            font-size: 12px;
            font-weight: 700;
            color: #667eea;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        .file-count-badge {
            display: inline-block;
            background: #667eea;
            color: white;
            font-size: 11px;
            font-weight: 700;
            padding: 2px 8px;
            border-radius: 10px;
            margin-left: 6px;
        }
        .clear-btn {
            background: none;
            border: none;
            color: #aaa;
            font-size: 12px;
            cursor: pointer;
            padding: 0;
            font-weight: 600;
        }
        .clear-btn:hover { color: #e74c3c; }
        .file-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 7px 0;
            border-bottom: 1px solid #eef0ff;
            font-size: 13px;
            color: #444;
        }
        .file-item:last-child { border-bottom: none; }
        .file-item-name {
            font-weight: 600;
            color: #333;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 68%;
        }
        .file-item-size {
            color: #667eea;
            font-size: 12px;
            font-weight: 700;
            background: #eef1ff;
            padding: 2px 8px;
            border-radius: 10px;
            white-space: nowrap;
        }
        .file-list-footer {
            margin-top: 8px;
            padding-top: 8px;
            border-top: 2px solid #eef0ff;
            font-size: 12px;
            font-weight: 700;
            display: flex;
            justify-content: space-between;
        }

        /* ── Progress ─────────────────────────────────────────────────── */
        input[type=file] { display: none; }
        .progress-wrap {
            display: none;
            background: #e9ecef;
            border-radius: 6px;
            height: 10px;
            margin-bottom: 16px;
            overflow: hidden;
        }
        .progress-bar {
            height: 100%;
            background: linear-gradient(90deg, #667eea, #764ba2);
            width: 0%;
            border-radius: 6px;
            transition: width 0.3s;
        }

        /* ── Upload button ────────────────────────────────────────────── */
        .btn-upload {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border: none;
            padding: 13px 28px;
            color: white;
            font-size: 15px;
            font-weight: 600;
            border-radius: 8px;
            cursor: pointer;
            width: 100%;
            transition: opacity 0.2s;
        }
        .btn-upload:hover { opacity: 0.92; }
        .btn-upload:disabled { opacity: 0.6; cursor: not-allowed; }

        /* ── Info box ─────────────────────────────────────────────────── */
        .info-box {
            background: #f0f4ff;
            border: 1px solid #c4d0f7;
            border-radius: 8px;
            padding: 14px 16px;
            margin-top: 20px;
            font-size: 13px;
            color: #4a5568;
            line-height: 1.7;
        }
        .info-box strong { color: #667eea; }
    </style>
</head>
<body>

    <div class="navbar">
        <div>&#9729;&#65039; J-Cloud</div>
        <div>
            <a href="<%= request.getContextPath() %>/dashboard.jsp">Dashboard</a>
            <a href="<%= request.getContextPath() %>/logout">Logout</a>
        </div>
    </div>

    <div class="container">
        <div class="card">
            <h1>&#128228; Upload Files</h1>
            <p class="subtitle">
                Select one or more files — each will be split into 5 MB chunks
                and distributed across active data nodes.
            </p>

            <!-- Success / Error messages from servlet -->
            <% if (request.getAttribute("success") != null) { %>
                <div class="message success">&#10003; <%= request.getAttribute("success") %></div>
            <% } %>
            <% if (request.getAttribute("error") != null) { %>
                <div class="message error">&#9888; <%= request.getAttribute("error") %></div>
            <% } %>

            <!-- Limit badges -->
            <div class="limit-row">
                <span class="limit-badge">&#128194; Max 5 files per upload</span>
                <span class="limit-badge">&#128190; Max 50 MB total</span>
            </div>

            <form id="uploadForm" method="post" enctype="multipart/form-data"
                  action="<%= request.getContextPath() %>/upload">

                <!-- Drop zone -->
                <div class="drop-zone" id="dropZone"
                     onclick="document.getElementById('fileInput').click()">
                    <div class="icon">&#128193;</div>
                    <p>Drag &amp; drop files here, or <span>browse</span></p>
                    <p class="hint">Up to 5 files &bull; 50 MB total maximum</p>
                </div>

                <!-- Hidden multi-file input -->
                <input type="file" name="files" id="fileInput" multiple>

                <!-- Selected file list (shown after selection) -->
                <div class="file-list" id="fileList">
                    <div class="file-list-header">
                        <div class="file-list-title">
                            Selected files
                            <span class="file-count-badge" id="fileCount">0</span>
                        </div>
                        <button type="button" class="clear-btn" onclick="clearFiles()">
                            ✕ Clear all
                        </button>
                    </div>
                    <div id="fileItems"></div>
                </div>

                <!-- Progress bar -->
                <div class="progress-wrap" id="progressWrap">
                    <div class="progress-bar" id="progressBar"></div>
                </div>

                <!-- Upload button -->
                <button type="submit" class="btn-upload" id="uploadBtn">
                    &#128228; Upload &amp; Distribute
                </button>

            </form>

            <div class="info-box">
                <strong>How it works:</strong><br>
                Each file is split into 5 MB chunks and sent to data nodes in parallel
                using Java threads. Metadata is stored in PostgreSQL so every file can be
                reassembled on download. Multiple files are processed one by one, each
                fully distributed with a replication factor of 2.
            </div>
        </div>
    </div>

    <script>
        // ── Limits (must match servlet constants) ───────────────────────────
        const MAX_FILES           = 5;
        const MAX_TOTAL_SIZE_MB   = 50;
        const MAX_TOTAL_SIZE_BYTES = MAX_TOTAL_SIZE_MB * 1024 * 1024;

        // ── DOM refs ────────────────────────────────────────────────────────
        const dropZone     = document.getElementById('dropZone');
        const fileInput    = document.getElementById('fileInput');
        const fileList     = document.getElementById('fileList');
        const fileItems    = document.getElementById('fileItems');
        const fileCount    = document.getElementById('fileCount');
        const uploadBtn    = document.getElementById('uploadBtn');
        const progressWrap = document.getElementById('progressWrap');
        const progressBar  = document.getElementById('progressBar');

        // ── File selection handler ──────────────────────────────────────────
        fileInput.addEventListener('change', renderFileList);

        // ── Render selected file list with live limit feedback ──────────────
        function renderFileList() {
            const files = fileInput.files;
            if (!files || files.length === 0) {
                fileList.style.display = 'none';
                dropZone.classList.remove('has-error');
                uploadBtn.textContent = '📤 Upload & Distribute';
                return;
            }

            let totalBytes = 0;
            fileItems.innerHTML = '';

            for (let i = 0; i < files.length; i++) {
                totalBytes += files[i].size;
                const div = document.createElement('div');
                div.className = 'file-item';
                div.innerHTML =
                    '<span class="file-item-name">&#128196; '
                    + escapeHtml(files[i].name) + '</span>'
                    + '<span class="file-item-size">'
                    + formatSize(files[i].size) + '</span>';
                fileItems.appendChild(div);
            }

            // Evaluate limits
            const overCount = files.length > MAX_FILES;
            const overSize  = totalBytes > MAX_TOTAL_SIZE_BYTES;
            const hasError  = overCount || overSize;

            // Footer row showing totals + warnings
            const footer = document.createElement('div');
            footer.className = 'file-list-footer';
            footer.innerHTML =
                '<span style="color:' + (overCount ? '#e74c3c' : '#555') + ';">'
                    + files.length + ' file' + (files.length > 1 ? 's' : '')
                    + (overCount
                        ? ' &nbsp;⚠ max ' + MAX_FILES + ' files'
                        : ' &nbsp;✓')
                + '</span>'
                + '<span style="color:' + (overSize ? '#e74c3c' : '#27ae60') + ';">'
                    + 'Total: ' + formatSize(totalBytes)
                    + (overSize
                        ? ' &nbsp;⚠ exceeds ' + MAX_TOTAL_SIZE_MB + ' MB'
                        : ' &nbsp;✓')
                + '</span>';
            fileItems.appendChild(footer);

            fileCount.textContent = files.length;
            fileList.style.display = 'block';

            // Highlight drop zone red if over limit
            if (hasError) {
                dropZone.classList.add('has-error');
            } else {
                dropZone.classList.remove('has-error');
            }

            // Update button label
            uploadBtn.textContent = files.length === 1
                ? '📤 Upload 1 File'
                : '📤 Upload ' + files.length + ' Files';
        }

        // ── Clear selection ─────────────────────────────────────────────────
        function clearFiles() {
            fileInput.value = '';
            fileList.style.display  = 'none';
            fileItems.innerHTML     = '';
            dropZone.classList.remove('has-error');
            uploadBtn.textContent   = '📤 Upload & Distribute';
        }

        // ── Drag and drop ───────────────────────────────────────────────────
        dropZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropZone.classList.add('drag-over');
        });
        dropZone.addEventListener('dragleave', () => {
            dropZone.classList.remove('drag-over');
        });
        dropZone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropZone.classList.remove('drag-over');
            if (e.dataTransfer.files.length > 0) {
                fileInput.files = e.dataTransfer.files;
                renderFileList();
            }
        });

        // ── Submit handler with client-side validation ──────────────────────
        document.getElementById('uploadForm').addEventListener('submit', function(e) {
            if (!fileInput.files || fileInput.files.length === 0) return;

            // Count check
            if (fileInput.files.length > MAX_FILES) {
                e.preventDefault();
                alert('Too many files selected.\nMaximum ' + MAX_FILES
                    + ' files are allowed per upload.');
                return;
            }

            // Total size check
            let totalBytes = 0;
            for (let i = 0; i < fileInput.files.length; i++) {
                totalBytes += fileInput.files[i].size;
            }
            if (totalBytes > MAX_TOTAL_SIZE_BYTES) {
                e.preventDefault();
                const totalMB = (totalBytes / (1024 * 1024)).toFixed(1);
                alert('Total size (' + totalMB + ' MB) exceeds the '
                    + MAX_TOTAL_SIZE_MB + ' MB limit.\nPlease reduce your selection.');
                return;
            }

            // All checks passed — lock UI and show progress
            uploadBtn.disabled = true;
            const n = fileInput.files.length;
            uploadBtn.textContent = 'Uploading ' + n
                + ' file' + (n > 1 ? 's' : '') + '... please wait';
            progressWrap.style.display = 'block';
            let w = 0;
            setInterval(() => {
                w = Math.min(w + Math.random() * 4, 88);
                progressBar.style.width = w + '%';
            }, 400);
        });

        // ── Helpers ─────────────────────────────────────────────────────────
        function formatSize(bytes) {
            if (bytes >= 1024 * 1024 * 1024)
                return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
            if (bytes >= 1024 * 1024)
                return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
            if (bytes >= 1024)
                return (bytes / 1024).toFixed(1) + ' KB';
            return bytes + ' B';
        }

        function escapeHtml(str) {
            return str
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;');
        }
    </script>
</body>
</html>