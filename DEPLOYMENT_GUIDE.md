# J-Cloud - Setup & Deployment Guide

## 🎯 What We Built - All 4 Phases Complete!

### ✅ Phase 1: Database & Shared Layer
- **DBConnection.java** - Thread-safe Singleton for MySQL connections
- **UserDAO.java** - User registration and authentication
- **NodeDAO.java** - Node registration and status management
- **Shared POJOs** - User, NodeInfo, Chunk, ChunkLocation, FileMetadata (with getters/setters)

### ✅ Phase 2: Scalable Master Node
- **MasterServer.java** - Main server with 50-thread pool
- **ClientHandler.java** - Protocol parser (REGISTER_NODE, HEARTBEAT, PING)
- **HeartbeatMonitor.java** - Scheduled death detector (15s timeout)
- ConcurrentHashMap for thread-safe node tracking

### ✅ Phase 3: Data Node Development
- **DataNodeServer.java** - Auto-registration with Master
- **DataNode2Launcher.java** - Quick launcher for second node
- Scheduled heartbeat every 5 seconds
- Graceful shutdown hooks

### ✅ Phase 4: Tomcat Web Application
- **RegisterServlet.java** - User registration with MD5 hashing
- **LoginServlet.java** - Authentication with session management
- **LogoutServlet.java** - Session invalidation
- **register.jsp, login.jsp, dashboard.jsp** - Beautiful UI pages
- **web.xml** - Tomcat configuration

---

## 📋 Prerequisites

Before you start, ensure you have:

1. **Java JDK 8+** - [Download here](https://www.oracle.com/java/technologies/downloads/)
2. **MySQL Server** - Already configured with:
   - Database: `jcloud`
   - Username: `root`
   - Password: `Jcloud@db`
   - Port: `3306`
3. **Apache Tomcat 9.0+** - [Download here](https://tomcat.apache.org/download-90.cgi)
4. **MySQL JDBC Driver** - [Download here](https://dev.mysql.com/downloads/connector/j/)

---

## 🗄️ Step 1: Database Setup

Your friend already created the database, so just verify it exists:

```cmd
mysql -u root -pJcloud@db
```

```sql
USE jcloud;
SHOW TABLES;
-- You should see: users, files, chunks, nodes, chunk_locations
EXIT;
```

If nodes are not populated:
```sql
INSERT INTO nodes (node_name, ip_address, port, status, storage_capacity) 
VALUES 
  ('DataNode1', 'localhost', 9101, 'ACTIVE', 10737418240),
  ('DataNode2', 'localhost', 9102, 'ACTIVE', 10737418240);
```

---

## 🔧 Step 2: Project Structure

Your project should look like this:

```
J-Cloud/
├── shared/              # Shared POJOs
│   ├── User.java
│   ├── NodeInfo.java
│   ├── Chunk.java
│   ├── ChunkLocation.java
│   └── FileMetadata.java
├── utils/               # Utilities
│   └── DBConnection.java
├── dao/                 # Data Access Objects
│   ├── UserDAO.java
│   └── NodeDAO.java
├── master/              # Master Node
│   ├── MasterServer.java
│   ├── ClientHandler.java
│   └── HeartbeatMonitor.java
├── datanode/            # Data Nodes
│   ├── DataNodeServer.java
│   └── DataNode2Launcher.java
├── webapp/              # Web Application
│   ├── servlet/
│   │   ├── RegisterServlet.java
│   │   ├── LoginServlet.java
│   │   └── LogoutServlet.java
│   ├── WEB-INF/
│   │   └── web.xml
│   ├── index.jsp
│   ├── register.jsp
│   ├── login.jsp
│   └── dashboard.jsp
└── database/
    └── schema.sql
```

---

## 🔨 Step 3: Compile the Project

### Option A: Using Command Line

```cmd
cd C:\Users\Pc\J-Cloud

# Add MySQL JDBC driver to classpath (download mysql-connector-java-8.x.x.jar)
set CLASSPATH=.;mysql-connector-java-8.0.33.jar

# Compile all Java files
javac -d bin shared/*.java
javac -d bin -cp bin utils/*.java
javac -d bin -cp bin dao/*.java
javac -d bin -cp bin master/*.java
javac -d bin -cp bin datanode/*.java
javac -d bin -cp bin webapp/servlet/*.java
```

### Option B: Using Eclipse/IntelliJ IDEA

1. Import project as Java project
2. Add MySQL JDBC driver to build path
3. Project will compile automatically

---

## 🚀 Step 4: Run the System (In Order!)

### **STEP 1: Start Master Node**

Open **Terminal 1**:
```cmd
cd C:\Users\Pc\J-Cloud
java -cp "bin;mysql-connector-java-8.0.33.jar" master.MasterServer
```

You should see:
```
╔════════════════════════════════════════════╗
║   J-CLOUD MASTER NODE STARTED              ║
║   Port: 9000                               ║
║   Thread Pool Size: 50                     ║
╚════════════════════════════════════════════╝

✓ Database connection established successfully
✓ Heartbeat monitor started (check interval: 10s, timeout: 15s)
```

### **STEP 2: Start Data Node 1**

Open **Terminal 2**:
```cmd
cd C:\Users\Pc\J-Cloud
java -cp "bin;mysql-connector-java-8.0.33.jar" datanode.DataNodeServer DataNode1 9101
```

You should see:
```
╔════════════════════════════════════════════╗
║   J-CLOUD DATA NODE STARTING               ║
║   Node: DataNode1                          ║
║   Port: 9101                               ║
╚════════════════════════════════════════════╝

→ Registering with Master Node at localhost:9000
  Sent: REGISTER_NODE|DataNode1|localhost|9101|10737418240
  Response: OK|Node registered successfully
✓ Registration successful!

♥ Starting heartbeat service (interval: 5s)
```

### **STEP 3: Start Data Node 2**

Open **Terminal 3**:
```cmd
cd C:\Users\Pc\J-Cloud
java -cp "bin;mysql-connector-java-8.0.33.jar" datanode.DataNode2Launcher
```

### **STEP 4: Deploy Web Application to Tomcat**

1. **Create WAR file structure:**
   ```
   jcloud.war/
   ├── WEB-INF/
   │   ├── web.xml
   │   ├── classes/
   │   │   ├── shared/
   │   │   ├── utils/
   │   │   ├── dao/
   │   │   └── servlet/
   │   └── lib/
   │       └── mysql-connector-java-8.0.33.jar
   ├── index.jsp
   ├── register.jsp
   ├── login.jsp
   └── dashboard.jsp
   ```

2. **Copy compiled classes:**
   ```cmd
   mkdir jcloud\WEB-INF\classes
   xcopy /E /I bin\shared jcloud\WEB-INF\classes\shared
   xcopy /E /I bin\utils jcloud\WEB-INF\classes\utils
   xcopy /E /I bin\dao jcloud\WEB-INF\classes\dao
   xcopy /E /I bin\servlet jcloud\WEB-INF\classes\servlet
   ```

3. **Copy JSP and config files:**
   ```cmd
   copy webapp\*.jsp jcloud\
   copy webapp\WEB-INF\web.xml jcloud\WEB-INF\
   copy mysql-connector-java-8.0.33.jar jcloud\WEB-INF\lib\
   ```

4. **Deploy to Tomcat:**
   - Copy `jcloud` folder to `%CATALINA_HOME%\webapps\`
   - Start Tomcat: `%CATALINA_HOME%\bin\startup.bat`

---

## 🧪 Step 5: Test the System

### Test 1: Verify Master Node Health
Open browser: `http://localhost:9000`
Or use telnet:
```cmd
telnet localhost 9000
PING
```
Should respond: `PONG`

### Test 2: Check Database Nodes
```sql
SELECT * FROM nodes;
-- Should show DataNode1 and DataNode2 as ACTIVE
```

### Test 3: Test Heartbeat Death Detection
- Kill one Data Node (Ctrl+C in its terminal)
- Wait 15 seconds
- Master should mark it as DEAD
- Check: `SELECT * FROM nodes;`

### Test 4: Test Web Application

1. **Access the app:** `http://localhost:8080/jcloud`
2. **Register a new user:**
   - Username: `testuser`
   - Email: `test@jcloud.com`
   - Password: `test123`
3. **Login with credentials**
4. **View dashboard**

---

## 📊 System Architecture

```
┌─────────────────────────────────────────────────────┐
│                  TOMCAT (8080)                      │
│   ┌────────────┐  ┌──────────┐  ┌──────────────┐   │
│   │ Register   │  │  Login   │  │  Dashboard   │   │
│   │  Servlet   │  │ Servlet  │  │     JSP      │   │
│   └─────┬──────┘  └────┬─────┘  └──────────────┘   │
└─────────┼───────────────┼──────────────────────────┘
          │               │
          └───────┬───────┘
                  │
         ┌────────▼─────────┐
         │   UserDAO        │
         │   (Thread-safe)  │
         └────────┬─────────┘
                  │
         ┌────────▼──────────┐
         │   DBConnection    │
         │   (Singleton)     │
         └────────┬──────────┘
                  │
         ┌────────▼──────────┐
         │  MySQL Database   │
         │   (localhost)     │
         └───────────────────┘

┌────────────────────────────────────────────────────┐
│           MASTER NODE (9000)                       │
│   ┌──────────────────────────────────────────┐    │
│   │  Thread Pool (50 threads)                │    │
│   │  ┌────────────┐  ┌──────────────────┐    │    │
│   │  │  Client    │  │   Heartbeat      │    │    │
│   │  │  Handler   │  │   Monitor        │    │    │
│   │  └────────────┘  └──────────────────┘    │    │
│   └──────────────────────────────────────────┘    │
│   ConcurrentHashMap<Node, Timestamp>              │
└───────────┬───────────────────┬───────────────────┘
            │                   │
    ┌───────▼──────┐    ┌──────▼──────┐
    │  Data Node 1 │    │ Data Node 2 │
    │ (Port 9101)  │    │ (Port 9102) │
    │              │    │             │
    │ ♥ Heartbeat  │    │ ♥ Heartbeat │
    │   (5s)       │    │   (5s)      │
    └──────────────┘    └─────────────┘
```

---

## 🔑 Key Configuration

All configuration is centralized in `DBConnection.java`:

```java
DB_URL = "jdbc:mysql://localhost:3306/jcloud"
DB_USER = "root"
DB_PASSWORD = "Jcloud@db"
```

Master Node: `localhost:9000`
Data Node 1: `localhost:9101`
Data Node 2: `localhost:9102`
MySQL: `localhost:3306`
Tomcat: `localhost:8080`

---

## 🐛 Troubleshooting

### Issue: "Cannot connect to database"
- Verify MySQL is running: `net start MySQL80`
- Check credentials: `mysql -u root -pJcloud@db`
- Ensure JDBC driver is in classpath

### Issue: "Master Node connection refused"
- Ensure Master Node is started first
- Check port 9000 is not blocked by firewall
- Verify Master console shows "STARTED"

### Issue: "Node marked as DEAD"
- Check Data Node is running
- Verify heartbeat messages in Master console
- Ensure network connectivity

### Issue: "Tomcat 404 error"
- Verify WAR deployed to `webapps/jcloud`
- Check Tomcat logs in `logs/catalina.out`
- Ensure MySQL JDBC driver in `WEB-INF/lib/`

---

## 📈 Next Steps

Now that core infrastructure is running, you can implement:

1. **File Upload/Download** - Chunking algorithm and storage
2. **Replication Logic** - Store chunks across multiple nodes
3. **Load Balancing** - Distribute chunks evenly
4. **Node Recovery** - Re-replicate chunks when node dies
5. **Web UI** - File browser, upload/download interfaces

---

## 🎉 Success Indicators

When everything is working:

✅ Master Node shows heartbeat monitor running
✅ Both Data Nodes show "Heartbeat sent and acknowledged"
✅ MySQL `nodes` table shows both nodes as ACTIVE
✅ Tomcat accessible at http://localhost:8080
✅ Can register and login via web interface
✅ Dashboard displays user information

---

## 💡 Development Tips

- **Use separate terminals** for each component (easier debugging)
- **Check logs** in each terminal for errors
- **Test incrementally** - don't start everything at once
- **Database first** - always verify DB before starting nodes
- **Monitor heartbeats** - key indicator of system health

---

Built with ❤️ using scalable Java patterns: Thread Pools, Connection Pooling, Scheduled Executors, and Concurrent Data Structures.
