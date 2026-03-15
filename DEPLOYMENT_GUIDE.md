# J-Cloud - Setup & Deployment Guide

## ðŸŽ¯ What We Built - All 4 Phases Complete!

### âœ… Phase 1: Database & Shared Layer
- **DBConnection.java** - Thread-safe Singleton for PostgreSQL connections
- **UserDAO.java** - User registration and authentication
- **NodeDAO.java** - Node registration and status management
- **Shared POJOs** - User, NodeInfo, Chunk, ChunkLocation, FileMetadata (with getters/setters)

### âœ… Phase 2: Scalable Master Node
- **MasterServer.java** - Main server with 50-thread pool
- **ClientHandler.java** - Protocol parser (REGISTER_NODE, HEARTBEAT, PING)
- **HeartbeatMonitor.java** - Scheduled death detector (15s timeout)
- ConcurrentHashMap for thread-safe node tracking

### âœ… Phase 3: Data Node Development
- **DataNodeServer.java** - Auto-registration with Master
- **DataNode2Launcher.java** - Quick launcher for second node
- Scheduled heartbeat every 5 seconds
- Graceful shutdown hooks

### âœ… Phase 4: Tomcat Web Application
- **RegisterServlet.java** - User registration with MD5 hashing
- **LoginServlet.java** - Authentication with session management
- **LogoutServlet.java** - Session invalidation
- **register.jsp, login.jsp, dashboard.jsp** - Beautiful UI pages
- **web.xml** - Tomcat configuration

---

## ðŸ“‹ Prerequisites

Before you start, ensure you have:

1. **Java JDK 8+** - [Download here](https://www.oracle.com/java/technologies/downloads/)
2. **PostgreSQL Server** - Already configured with:
   - Database: `jcloud`
   - Username: `root`
   - Password: `Jcloud@db`
   - Port: `5432`
3. **Apache Tomcat 9.0+** - [Download here](https://tomcat.apache.org/download-90.cgi)
4. **PostgreSQL JDBC Driver** - [Download here](https://dev.PostgreSQL.com/downloads/connector/j/)

---

## ðŸ—„ï¸ Step 1: Database Setup

Your friend already created the database, so just verify it exists:

```cmd
Use Neon SQL Editor or psql with JCLOUD_DB_URL
```

```sql
-- Use Neon database selected in JCLOUD_DB_URL
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

## ðŸ”§ Step 2: Project Structure

Your project should look like this:

```
J-Cloud/
â”œâ”€â”€ shared/              # Shared POJOs
â”‚   â”œâ”€â”€ User.java
â”‚   â”œâ”€â”€ NodeInfo.java
â”‚   â”œâ”€â”€ Chunk.java
â”‚   â”œâ”€â”€ ChunkLocation.java
â”‚   â””â”€â”€ FileMetadata.java
â”œâ”€â”€ utils/               # Utilities
â”‚   â””â”€â”€ DBConnection.java
â”œâ”€â”€ dao/                 # Data Access Objects
â”‚   â”œâ”€â”€ UserDAO.java
â”‚   â””â”€â”€ NodeDAO.java
â”œâ”€â”€ master/              # Master Node
â”‚   â”œâ”€â”€ MasterServer.java
â”‚   â”œâ”€â”€ ClientHandler.java
â”‚   â””â”€â”€ HeartbeatMonitor.java
â”œâ”€â”€ datanode/            # Data Nodes
â”‚   â”œâ”€â”€ DataNodeServer.java
â”‚   â””â”€â”€ DataNode2Launcher.java
â”œâ”€â”€ webapp/              # Web Application
â”‚   â”œâ”€â”€ servlet/
â”‚   â”‚   â”œâ”€â”€ RegisterServlet.java
â”‚   â”‚   â”œâ”€â”€ LoginServlet.java
â”‚   â”‚   â””â”€â”€ LogoutServlet.java
â”‚   â”œâ”€â”€ WEB-INF/
â”‚   â”‚   â””â”€â”€ web.xml
â”‚   â”œâ”€â”€ index.jsp
â”‚   â”œâ”€â”€ register.jsp
â”‚   â”œâ”€â”€ login.jsp
â”‚   â””â”€â”€ dashboard.jsp
â””â”€â”€ database/
    â””â”€â”€ schema.sql
```

---

## ðŸ”¨ Step 3: Compile the Project

### Option A: Using Command Line

```cmd
cd C:\Users\Pc\J-Cloud

# Add PostgreSQL JDBC driver to classpath (download postgresql-42.7.10.jar)
set CLASSPATH=.;postgresql-42.7.10.jar

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
2. Add PostgreSQL JDBC driver to build path
3. Project will compile automatically

---

## ðŸš€ Step 4: Run the System (In Order!)

### **STEP 1: Start Master Node**

Open **Terminal 1**:
```cmd
cd C:\Users\Pc\J-Cloud
java -cp "bin;postgresql-42.7.10.jar" master.MasterServer
```

You should see:
```
â•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—
â•‘   J-CLOUD MASTER NODE STARTED              â•‘
â•‘   Port: 9000                               â•‘
â•‘   Thread Pool Size: 50                     â•‘
â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

âœ“ Database connection established successfully
âœ“ Heartbeat monitor started (check interval: 10s, timeout: 15s)
```

### **STEP 2: Start Data Node 1**

Open **Terminal 2**:
```cmd
cd C:\Users\Pc\J-Cloud
java -cp "bin;postgresql-42.7.10.jar" datanode.DataNodeServer DataNode1 9101
```

You should see:
```
â•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—
â•‘   J-CLOUD DATA NODE STARTING               â•‘
â•‘   Node: DataNode1                          â•‘
â•‘   Port: 9101                               â•‘
â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

â†’ Registering with Master Node at localhost:9000
  Sent: REGISTER_NODE|DataNode1|localhost|9101|10737418240
  Response: OK|Node registered successfully
âœ“ Registration successful!

â™¥ Starting heartbeat service (interval: 5s)
```

### **STEP 3: Start Data Node 2**

Open **Terminal 3**:
```cmd
cd C:\Users\Pc\J-Cloud
java -cp "bin;postgresql-42.7.10.jar" datanode.DataNode2Launcher
```

### **STEP 4: Deploy Web Application to Tomcat**

1. **Create WAR file structure:**
   ```
   jcloud.war/
   â”œâ”€â”€ WEB-INF/
   â”‚   â”œâ”€â”€ web.xml
   â”‚   â”œâ”€â”€ classes/
   â”‚   â”‚   â”œâ”€â”€ shared/
   â”‚   â”‚   â”œâ”€â”€ utils/
   â”‚   â”‚   â”œâ”€â”€ dao/
   â”‚   â”‚   â””â”€â”€ servlet/
   â”‚   â””â”€â”€ lib/
   â”‚       â””â”€â”€ postgresql-42.7.10.jar
   â”œâ”€â”€ index.jsp
   â”œâ”€â”€ register.jsp
   â”œâ”€â”€ login.jsp
   â””â”€â”€ dashboard.jsp
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
   copy postgresql-42.7.10.jar jcloud\WEB-INF\lib\
   ```

4. **Deploy to Tomcat:**
   - Copy `jcloud` folder to `%CATALINA_HOME%\webapps\`
   - Start Tomcat: `%CATALINA_HOME%\bin\startup.bat`

---

## ðŸ§ª Step 5: Test the System

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

## ðŸ“Š System Architecture

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                  TOMCAT (8080)                      â”‚
â”‚   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”‚
â”‚   â”‚ Register   â”‚  â”‚  Login   â”‚  â”‚  Dashboard   â”‚   â”‚
â”‚   â”‚  Servlet   â”‚  â”‚ Servlet  â”‚  â”‚     JSP      â”‚   â”‚
â”‚   â””â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
          â”‚               â”‚
          â””â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”˜
                  â”‚
         â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
         â”‚   UserDAO        â”‚
         â”‚   (Thread-safe)  â”‚
         â””â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                  â”‚
         â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
         â”‚   DBConnection    â”‚
         â”‚   (Singleton)     â”‚
         â””â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                  â”‚
         â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
         â”‚  PostgreSQL Database   â”‚
         â”‚   (localhost)     â”‚
         â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜

â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚           MASTER NODE (9000)                       â”‚
â”‚   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”    â”‚
â”‚   â”‚  Thread Pool (50 threads)                â”‚    â”‚
â”‚   â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”    â”‚    â”‚
â”‚   â”‚  â”‚  Client    â”‚  â”‚   Heartbeat      â”‚    â”‚    â”‚
â”‚   â”‚  â”‚  Handler   â”‚  â”‚   Monitor        â”‚    â”‚    â”‚
â”‚   â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜    â”‚    â”‚
â”‚   â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜    â”‚
â”‚   ConcurrentHashMap<Node, Timestamp>              â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
            â”‚                   â”‚
    â”Œâ”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”    â”Œâ”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”
    â”‚  Data Node 1 â”‚    â”‚ Data Node 2 â”‚
    â”‚ (Port 9101)  â”‚    â”‚ (Port 9102) â”‚
    â”‚              â”‚    â”‚             â”‚
    â”‚ â™¥ Heartbeat  â”‚    â”‚ â™¥ Heartbeat â”‚
    â”‚   (5s)       â”‚    â”‚   (5s)      â”‚
    â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜    â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

---

## ðŸ”‘ Key Configuration

All configuration is centralized in `DBConnection.java`:

```java
DB_URL = "jdbc:PostgreSQL://Neon via .env (JCLOUD_DB_URL)/jcloud"
DB_USER = "root"
DB_PASSWORD = "Jcloud@db"
```

Master Node: `localhost:9000`
Data Node 1: `localhost:9101`
Data Node 2: `localhost:9102`
PostgreSQL: `Neon via .env (JCLOUD_DB_URL)`
Tomcat: `localhost:8080`

---

## ðŸ› Troubleshooting

### Issue: "Cannot connect to database"
- Verify PostgreSQL is running: `No local DB service required (Neon cloud DB)`
- Check credentials: `Use Neon SQL Editor or psql with JCLOUD_DB_URL`
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
- Ensure PostgreSQL JDBC driver in `WEB-INF/lib/`

---

## ðŸ“ˆ Next Steps

Now that core infrastructure is running, you can implement:

1. **File Upload/Download** - Chunking algorithm and storage
2. **Replication Logic** - Store chunks across multiple nodes
3. **Load Balancing** - Distribute chunks evenly
4. **Node Recovery** - Re-replicate chunks when node dies
5. **Web UI** - File browser, upload/download interfaces

---

## ðŸŽ‰ Success Indicators

When everything is working:

âœ… Master Node shows heartbeat monitor running
âœ… Both Data Nodes show "Heartbeat sent and acknowledged"
âœ… PostgreSQL `nodes` table shows both nodes as ACTIVE
âœ… Tomcat accessible at http://localhost:8080
âœ… Can register and login via web interface
âœ… Dashboard displays user information

---

## ðŸ’¡ Development Tips

- **Use separate terminals** for each component (easier debugging)
- **Check logs** in each terminal for errors
- **Test incrementally** - don't start everything at once
- **Database first** - always verify DB before starting nodes
- **Monitor heartbeats** - key indicator of system health

---

Built with â¤ï¸ using scalable Java patterns: Thread Pools, Connection Pooling, Scheduled Executors, and Concurrent Data Structures.
