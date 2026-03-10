<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="shared.User" %>
<%
    // Session check - redirect to login if not authenticated
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
    <title>Dashboard - J-Cloud</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
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
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
        }
        
        .navbar h1 {
            font-size: 24px;
        }
        
        .user-info {
            display: flex;
            align-items: center;
            gap: 20px;
        }
        
        .logout-btn {
            background: rgba(255, 255, 255, 0.2);
            color: white;
            border: 1px solid rgba(255, 255, 255, 0.3);
            padding: 8px 20px;
            border-radius: 5px;
            text-decoration: none;
            transition: background 0.3s;
        }
        
        .logout-btn:hover {
            background: rgba(255, 255, 255, 0.3);
        }
        
        .container {
            max-width: 1200px;
            margin: 30px auto;
            padding: 0 20px;
        }
        
        .welcome-card {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
            margin-bottom: 30px;
        }
        
        .welcome-card h2 {
            color: #667eea;
            margin-bottom: 10px;
        }
        
        .welcome-card p {
            color: #666;
        }
        
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        
        .stat-card {
            background: white;
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
            text-align: center;
        }
        
        .stat-card .icon {
            font-size: 48px;
            margin-bottom: 10px;
        }
        
        .stat-card .number {
            font-size: 32px;
            font-weight: bold;
            color: #667eea;
            margin-bottom: 5px;
        }
        
        .stat-card .label {
            color: #666;
            font-size: 14px;
        }
        
        .actions-card {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        }
        
        .actions-card h3 {
            color: #333;
            margin-bottom: 20px;
        }
        
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
            border-radius: 5px;
            text-decoration: none;
            transition: transform 0.2s;
            display: block;
        }
        
        .action-btn:hover {
            transform: translateY(-2px);
        }
        
        .status-indicator {
            display: inline-block;
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background: #4caf50;
            margin-right: 5px;
        }
    </style>
</head>
<body>
    <div class="navbar">
        <h1>☁️ J-Cloud Dashboard</h1>
        <div class="user-info">
            <span>Welcome, <strong><%= user.getUsername() %></strong></span>
            <a href="logout" class="logout-btn">Logout</a>
        </div>
    </div>
    
    <div class="container">
        <div class="welcome-card">
            <h2>Welcome to J-Cloud!</h2>
            <p>Distributed File Storage System - Your files, securely stored across multiple nodes.</p>
            <p><span class="status-indicator"></span> System Status: Online</p>
        </div>
        
        <div class="stats-grid">
            <div class="stat-card">
                <div class="icon">📁</div>
                <div class="number">0</div>
                <div class="label">Files Uploaded</div>
            </div>
            
            <div class="stat-card">
                <div class="icon">💾</div>
                <div class="number">0 GB</div>
                <div class="label">Storage Used</div>
            </div>
            
            <div class="stat-card">
                <div class="icon">🖥️</div>
                <div class="number">2</div>
                <div class="label">Active Nodes</div>
            </div>
            
            <div class="stat-card">
                <div class="icon">⚡</div>
                <div class="number">0</div>
                <div class="label">Recent Activities</div>
            </div>
        </div>
        
        <div class="actions-card">
            <h3>Quick Actions</h3>
            <div class="action-buttons">
                <a href="#" class="action-btn">📤 Upload File</a>
                <a href="#" class="action-btn">📥 Download File</a>
                <a href="#" class="action-btn">📋 My Files</a>
                <a href="#" class="action-btn">⚙️ Settings</a>
            </div>
        </div>
    </div>
</body>
</html>
