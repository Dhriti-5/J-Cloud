# J-Cloud - Setup & Deployment Guide

## 🎯 What We Built - All 5 Phases Complete!

### ✅ Phase 1: Database & Shared Layer
- **DBConnection.java** - Thread-safe Singleton for PostgreSQL connections
- **UserDAO.java** - User registration and authentication
- **NodeDAO.java** - Node registration and status management
- **FileDAO.java** - File metadata management
- **ChunkDAO.java** - Chunk metadata management
- **ChunkLocationDAO.java** - Chunk-to-node mapping
- **Shared POJOs** - User, NodeInfo, Chunk, ChunkLocation, FileMetadata (with getters/setters)

### ✅ Phase 2: Scalable Master Node
- **MasterServer.java** - Main server with 50-thread pool
- **ClientHandler.java** - Protocol parser (REGISTER_NODE, HEARTBEAT, PING)
- **HeartbeatMonitor.java** - Scheduled death detector (15s timeout)
- ConcurrentHashMap for thread-safe node tracking

### ✅ Phase 3: Data Node Development
- **DataNodeServer.java** - Auto-registration with Master, chunk storage server
- **DataNode2Launcher.java** - Quick launcher for second node
- Scheduled heartbeat every 5 seconds
- Graceful shutdown hooks
- Chunk files stored as `chunk_fileId_chunkIndex_chunkId.dat` in `storage/` folder

### ✅ Phase 4: Tomcat Web Application
- **RegisterServlet.java** - User registration with MD5 hashing
- **LoginServlet.java** - Authentication with session management
- **LogoutServlet.java** - Session invalidation
- **register.jsp, login.jsp, dashboard.jsp** - Beautiful UI pages
- **web.xml** - Tomcat configuration

### ✅ Phase 5: File Upload (Day 5)
- **UploadServlet.java** - Chunked parallel file upload to data nodes
- **upload.jsp** - Drag and drop upload UI with progress bar
- **dashboard.jsp** - Updated with live file count, storage used, active node list
- Round-robin chunk distribution using Java ExecutorService

---

## 📋 Prerequisites

Before you start, ensure you have:

1. **Java JDK 8+** - [Download here](https://www.oracle.com/java/technologies/downloads/)
2. **Neon PostgreSQL** - Cloud database configured via `.env` file
3. **Apache Tomcat 9.0+** - Already available inside `%TOMCAT_HOME%` folder in the repo
4. **PostgreSQL JDBC Driver** - `webapp\WEB-INF\lib\postgresql-42.7.10.jar`
5. **servlet-api.jar** - In project root `D:\j-cloud\servlet-api.jar`

---

## 🗄️ Step 1: Database Setup

Your `.env` file must exist at `D:\j-cloud\.env`:
```
JCLOUD_DB_URL=postgresql://user:password@host/neondb?sslmode=require&channel_binding=require
```

Verify tables in Neon SQL Editor:
```sql
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
-- Should show: users, files, chunks, nodes, chunk_locations
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
├── utils/               # DBConnection singleton
├── dao/                 # Data Access Objects
├── master/              # Master Node
├── datanode/            # Data Nodes
├── webapp/              # Web Application (edit files here)
│   ├── servlet/
│   │   ├── LoginServlet.java
│   │   ├── LogoutServlet.java
│   │   ├── RegisterServlet.java
│   │   └── UploadServlet.java
│   ├── WEB-INF/
│   │   ├── web.xml
│   │   └── lib/
│   │       └── postgresql-42.7.10.jar
│   ├── index.jsp
│   ├── login.jsp
│   ├── register.jsp
│   ├── dashboard.jsp
│   └── upload.jsp
├── %TOMCAT_HOME%/       # Tomcat — deployment goes here
│   └── webapps/
│       └── jcloud/
│           ├── WEB-INF/
│           │   ├── classes/
│           │   │   ├── shared/
│           │   │   ├── utils/
│           │   │   ├── dao/
│           │   │   └── servlet/
│           │   ├── lib/
│           │   │   └── postgresql-42.7.10.jar
│           │   └── web.xml
│           ├── index.jsp
│           ├── login.jsp
│           ├── register.jsp
│           ├── dashboard.jsp
│           └── upload.jsp
├── database/
│   └── schema_postgres.sql
├── bin/                 # Compiled classes
├── storage/             # Chunk .dat files
└── servlet-api.jar
```

---

## 🔨 Step 3: Compile the Project

Run `compile.bat` from `D:\j-cloud`:
```powershell
compile.bat
```

`compile.bat` automatically:
- Finds the PostgreSQL JDBC driver
- Finds `servlet-api.jar` from project root or Tomcat
- Compiles shared, utils, dao, master, datanode, and servlet classes
- Outputs compiled `.class` files to `bin\`

---

## 🚀 Step 4: Deploy to `%TOMCAT_HOME%`

Copy compiled classes and JSPs into the `%TOMCAT_HOME%` folder that already exists in the repo:
```powershell
Copy-Item -Recurse -Force bin\shared\*   "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\shared\"
Copy-Item -Recurse -Force bin\utils\*    "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\utils\"
Copy-Item -Recurse -Force bin\dao\*      "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\dao\"
Copy-Item -Recurse -Force bin\servlet\*  "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\servlet\"
Copy-Item -Force "webapp\WEB-INF\lib\postgresql-42.7.10.jar" "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\lib\"
Copy-Item -Force "webapp\WEB-INF\web.xml"                    "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\web.xml"
Copy-Item -Force webapp\index.jsp     "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\index.jsp"
Copy-Item -Force webapp\login.jsp     "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\login.jsp"
Copy-Item -Force webapp\register.jsp  "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\register.jsp"
Copy-Item -Force webapp\dashboard.jsp "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\dashboard.jsp"
Copy-Item -Force webapp\upload.jsp    "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\upload.jsp"
```

---

## 🚀 Step 5: Run the System (In Order!)

Open **4 separate PowerShell terminals**, all from `D:\j-cloud`:

### STEP 1: Start Master Node

**Terminal 1:**
```powershell
./run-master.bat
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

### STEP 2: Start Data Node 1

**Terminal 2:**
```powershell
./run-datanode1.bat
```
You should see:
```
→ Registering with Master Node at localhost:9000
  Sent: REGISTER_NODE|DataNode1|localhost|9101|10737418240
  Response: OK|Node registered successfully
✓ Registration successful!

♥ Starting heartbeat service (interval: 5s)
♥ Heartbeat sent and acknowledged
```

### STEP 3: Start Data Node 2

**Terminal 3:**
```powershell
./run-datanode2.bat
```

### STEP 4: Start Tomcat

**Terminal 4:**
```powershell
& "C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\startup.bat"
```

---

## 🧪 Step 5: Test the System

### Test 1: Check Database Nodes
```sql
SELECT * FROM nodes;
-- Should show DataNode1 and DataNode2 as ACTIVE
```

### Test 2: Test Heartbeat Death Detection
- Kill one Data Node (Ctrl+C in its terminal)
- Wait 15 seconds
- Master should mark it as DEAD
- Check: `SELECT * FROM nodes;`

### Test 3: Test Web Application

1. **Access the app:** `http://localhost:8080/jcloud`
2. **Register a new user**
3. **Login with credentials**
4. **View dashboard** — shows active nodes and file count
5. **Click Upload File**
6. **Select any file** and click Upload & Distribute
7. **Check DataNode terminals** — chunk `.dat` files are created in `storage/`
8. **Verify in DB:**
```sql
SELECT * FROM files;
SELECT * FROM chunks;
SELECT * FROM chunk_locations;
```

---

## 📊 System Architecture
```
┌─────────────────────────────────────────────────────┐
│              TOMCAT (port 8080)                      │
│  Register  Login  Dashboard  Upload                  │
│  Servlet   Servlet  JSP      Servlet                 │
└──────────────────┬──────────────────────────────────┘
                   │
         ┌─────────▼──────────┐
         │  DAOs              │
         │  UserDAO NodeDAO   │
         │  FileDAO ChunkDAO  │
         └─────────┬──────────┘
                   │
         ┌─────────▼──────────┐
         │   DBConnection     │
         │   (Singleton)      │
         └─────────┬──────────┘
                   │
         ┌─────────▼──────────┐
         │  Neon PostgreSQL   │
         │  (Cloud DB)        │
         └────────────────────┘

┌────────────────────────────────────────────────────┐
│           MASTER NODE (9000)                       │
│   Thread Pool (50) → ClientHandler                 │
│   HeartbeatMonitor (15s timeout)                   │
│   ConcurrentHashMap<Node, Timestamp>               │
└───────────┬────────────────────┬──────────────────┘
            │                    │
    ┌───────▼──────┐     ┌──────▼──────┐
    │ Data Node 1  │     │ Data Node 2 │
    │ (Port 9101)  │     │ (Port 9102) │
    │ ♥ HB (5s)   │     │ ♥ HB (5s)  │
    │ storage/     │     │ storage/    │
    └──────────────┘     └─────────────┘
```

---

## 🔑 Key Configuration

All DB configuration is via `.env` file in project root:
```
JCLOUD_DB_URL=postgresql://user:password@host/neondb?sslmode=require&channel_binding=require
```

| Component | Address |
|-----------|---------|
| Master Node | localhost:9000 |
| Data Node 1 | localhost:9101 |
| Data Node 2 | localhost:9102 |
| PostgreSQL | Neon via .env |
| Tomcat | localhost:8080 |

---

## 🛠 Troubleshooting

### Issue: "Port 9000 already in use"
```powershell
taskkill /IM java.exe /F
```
Then restart master.

### Issue: "No active data nodes available" on upload
Start `run-datanode1.bat` before uploading.

### Issue: "Cannot connect to database"
- Verify `.env` has correct `JCLOUD_DB_URL`
- Run `test-database.bat` to confirm connection

### Issue: "Master Node connection refused"
- Ensure Master Node is started first
- Check port 9000 is not blocked by firewall

### Issue: "Node marked as DEAD"
- Check Data Node is running
- Verify heartbeat messages in Master console

### Issue: "Tomcat 404 error on /upload"
- Re-run the deploy commands from Step 4
- Verify `UploadServlet.class` exists in `%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\servlet\`

### Issue: "compile.bat skips servlet compilation"
Ensure `servlet-api.jar` exists in project root `D:\j-cloud\servlet-api.jar`

---

## 📈 Next Steps

Now that core infrastructure and upload are working, you can implement:

1. ✅ File Upload — chunking and distribution
2. ⏳ File Download — DownloadServlet + chunk stitcher
3. ⏳ My Files page — list and manage uploaded files
4. ⏳ Replication Logic — store chunks on multiple nodes
5. ⏳ Node Recovery — re-replicate when node dies
6. ⏳ Load Balancing — distribute based on free space

---

## 🎉 Success Indicators

When everything is working:

✅ Master Node shows heartbeat monitor running
✅ Both Data Nodes show "Heartbeat sent and acknowledged"
✅ PostgreSQL `nodes` table shows both nodes as ACTIVE
✅ Tomcat accessible at `http://localhost:8080/jcloud`
✅ Can register and login via web interface
✅ Dashboard shows live file count and node status
✅ Upload File page works at `http://localhost:8080/jcloud/upload`
✅ After upload — chunk `.dat` files appear in `D:\j-cloud\storage\`
✅ After upload — rows in `files`, `chunks`, `chunk_locations` tables

---

## 💡 Development Tips

- **Use separate terminals** for each component (easier debugging)
- **Check logs** in each terminal for errors
- **Always start in order** — Master → DataNodes → Tomcat → Browser
- **Never upload before starting DataNodes** — you will get "No active nodes" error
- **Database first** — always verify DB before starting nodes
- **Monitor heartbeats** — key indicator of system health
- **PowerShell tip** — the folder is literally named `%TOMCAT_HOME%`, use it directly in paths

---

Built with ❤️ using scalable Java patterns: Thread Pools, Connection Pooling, Scheduled Executors, Round-Robin Distribution, and Concurrent Data Structures.