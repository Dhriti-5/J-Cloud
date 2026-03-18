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
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f5f7fa;
            margin: 0;
            padding: 0;
        }
        .navbar {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 15px 30px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
        }
        .navbar a {
            color: white;
            text-decoration: none;
            margin-left: 15px;
        }
        .container {
            max-width: 800px;
            margin: 40px auto;
            padding: 20px;
        }
        .card {
            background: white;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            padding: 30px;
        }
        h1 {
            color: #333;
            margin-bottom: 10px;
        }
        .message {
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 20px;
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
        .form-row {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 8px;
            font-weight: bold;
        }
        input[type=file] {
            width: 100%;
        }
        button {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border: none;
            padding: 12px 20px;
            color: white;
            font-size: 16px;
            border-radius: 8px;
            cursor: pointer;
        }
        button:hover {
            opacity: 0.95;
        }
    </style>
</head>
<body>
    <div class="navbar">
        <div>☁️ J-Cloud</div>
        <div>
            <a href="dashboard.jsp">Dashboard</a>
            <a href="logout">Logout</a>
        </div>
    </div>

    <div class="container">
        <div class="card">
            <h1>Upload File</h1>

            <% if (request.getAttribute("success") != null) { %>
                <div class="message success"><%= request.getAttribute("success") %></div>
            <% } %>

            <% if (request.getAttribute("error") != null) { %>
                <div class="message error"><%= request.getAttribute("error") %></div>
            <% } %>

            <form method="post" enctype="multipart/form-data" action="<%= request.getContextPath() %>/upload">
                <div class="form-row">
                    <label for="file">Choose a file</label>
                    <input type="file" name="file" id="file" required>
                </div>

                <button type="submit">Upload</button>
            </form>
        </div>
    </div>
</body>
</html>
