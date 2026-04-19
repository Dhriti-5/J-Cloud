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
    <title>Upload File - J-Cloud</title>
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
        .drop-zone {
            border: 2px dashed #c4c4e0;
            border-radius: 10px;
            padding: 48px 20px;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s;
            margin-bottom: 20px;
            background: #fafaff;
        }
        .drop-zone.drag-over {
            border-color: #667eea;
            background: #f0f0ff;
        }
        .drop-zone .icon { font-size: 42px; margin-bottom: 14px; }
        .drop-zone p { color: #666; font-size: 15px; }
        .drop-zone span { color: #667eea; font-weight: 600; }
        .file-chosen {
            margin-top: 12px;
            font-size: 13px;
            color: #667eea;
            font-weight: 600;
            display: none;
        }
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
            <h1>&#128228; Upload File</h1>
            <p class="subtitle">Your file will be split into 5 MB chunks and distributed across active data nodes.</p>

            <% if (request.getAttribute("success") != null) { %>
                <div class="message success">&#10003; <%= request.getAttribute("success") %></div>
            <% } %>
            <% if (request.getAttribute("error") != null) { %>
                <div class="message error">&#9888; <%= request.getAttribute("error") %></div>
            <% } %>

            <form id="uploadForm" method="post" enctype="multipart/form-data"
                  action="<%= request.getContextPath() %>/upload">

                <div class="drop-zone" id="dropZone"
                     onclick="document.getElementById('fileInput').click()">
                    <div class="icon">&#128193;</div>
                    <p>Drag &amp; drop a file here, or <span>browse</span></p>
                    <div class="file-chosen" id="fileChosen"></div>
                </div>
                <input type="file" name="file" id="fileInput" required>

                <div class="progress-wrap" id="progressWrap">
                    <div class="progress-bar" id="progressBar"></div>
                </div>

                <button type="submit" class="btn-upload" id="uploadBtn">
                    &#128228; Upload &amp; Distribute
                </button>
            </form>

            <div class="info-box">
                <strong>How it works:</strong><br>
                Files are split into 5 MB chunks and sent to different data nodes in parallel using Java threads.
                Metadata (filename, chunk locations, node IPs) is stored in PostgreSQL so the file can be
                reassembled on download.
            </div>
        </div>
    </div>

    <script>
        const dropZone   = document.getElementById('dropZone');
        const fileInput  = document.getElementById('fileInput');
        const fileChosen = document.getElementById('fileChosen');
        const uploadBtn  = document.getElementById('uploadBtn');
        const progressWrap = document.getElementById('progressWrap');
        const progressBar  = document.getElementById('progressBar');

        fileInput.addEventListener('change', showFile);

        function showFile() {
            if (fileInput.files[0]) {
                const f = fileInput.files[0];
                const sizeMB = (f.size / 1024 / 1024).toFixed(2);
                fileChosen.textContent = f.name + '  (' + sizeMB + ' MB)';
                fileChosen.style.display = 'block';
            }
        }

        dropZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropZone.classList.add('drag-over');
        });
        dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));
        dropZone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropZone.classList.remove('drag-over');
            if (e.dataTransfer.files.length > 0) {
                fileInput.files = e.dataTransfer.files;
                showFile();
            }
        });

        document.getElementById('uploadForm').addEventListener('submit', () => {
            if (!fileInput.files[0]) return;
            uploadBtn.disabled = true;
            uploadBtn.textContent = 'Uploading... please wait';
            progressWrap.style.display = 'block';
            let w = 0;
            const iv = setInterval(() => {
                w = Math.min(w + Math.random() * 6, 88);
                progressBar.style.width = w + '%';
            }, 400);
        });
    </script>
</body>
</html>