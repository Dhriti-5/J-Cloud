# J-Cloud - Setup & Deployment Guide

## 🎯 What We Built - All 8 Days Complete!

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

### ✅ Phase 6: File Download System (Day 8)
- **DownloadServlet.java** - Retrieves all chunks for a file in correct order, connects to each DataNode via TCP socket, and streams bytes sequentially into the HTTP response for a seamless browser download
- **ChunkLocationDAO.java** - Updated with method to look up which node holds each chunk
- **NodeDAO.java** - Updated with method to resolve a node ID into IP address and port
- **files.jsp** - My Files page listing all uploaded files with size, chunk count, and per-file download button
- **dashboard.jsp** - Download File and My Files buttons now active; Recent Files table with inline download links added directly on dashboard
- Fault-tolerant download — if a node is DEAD, next available replica is tried automatically

---

## 📋 Prerequisites

Before you start, ensure you have:

1. **Java JDK 8+** - [Download here](https://www.oracle.com/java/technologies/downloads/)
2. **Neon PostgreSQL** - Cloud database configured via `.env` file
3. **Apache Tomcat 9.0+** - Available inside `%TOMCAT_HOME%` folder in the repo, and installed at `C:\Program Files\Apache Software Foundation\Tomcat 9.0`
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
│   │   ├── UploadServlet.java
│   │   └── DownloadServlet.java   ← New Day 8
│   ├── WEB-INF/
│   │   ├── web.xml
│   │   └── lib/
│   │       └── postgresql-42.7.10.jar
│   ├── index.jsp
│   ├── login.jsp
│   ├── register.jsp
│   ├── dashboard.jsp              ← Updated Day 8
│   ├── upload.jsp
│   └── files.jsp                  ← New Day 8
├── %TOMCAT_HOME%/       # Tomcat — deployment goes here (Option A)
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
│           ├── upload.jsp
│           └── files.jsp
├── jcloud/              # Alternative deployment folder (Option B)
│   └── webapps/
│       └── jcloud/
│           ├── WEB-INF/
│           │   ├── classes/
│           │   └── lib/
│           ├── dashboard.jsp
│           ├── upload.jsp
│           └── files.jsp
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
cd D:\j-cloud
compile.bat
```

`compile.bat` automatically:
- Finds the PostgreSQL JDBC driver
- Finds `servlet-api.jar` from project root or Tomcat
- Compiles shared, utils, dao, master, datanode, and servlet classes (including `DownloadServlet.java`)
- Outputs compiled `.class` files to `bin\`

---

## 🚀 Step 4: Deploy to Tomcat

Three deploy paths are available — use whichever applies to your setup. All three end with the same result.

---

### ✅ Option A — Deploy via `%TOMCAT_HOME%` folder in the repo

```powershell
# Copy compiled classes
Copy-Item -Recurse -Force "D:\j-cloud\bin\shared\*"  "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\shared\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\utils\*"   "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\utils\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\dao\*"     "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\dao\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\servlet\*" "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\servlet\"

# Copy JDBC driver and web.xml
Copy-Item -Force "D:\j-cloud\webapp\WEB-INF\lib\postgresql-42.7.10.jar" "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\lib\"
Copy-Item -Force "D:\j-cloud\webapp\WEB-INF\web.xml"                    "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\web.xml"

# Copy all JSP files (including new files.jsp from Day 8)
Copy-Item -Force "D:\j-cloud\webapp\index.jsp"     "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\index.jsp"
Copy-Item -Force "D:\j-cloud\webapp\login.jsp"     "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\login.jsp"
Copy-Item -Force "D:\j-cloud\webapp\register.jsp"  "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\register.jsp"
Copy-Item -Force "D:\j-cloud\webapp\dashboard.jsp" "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\dashboard.jsp"
Copy-Item -Force "D:\j-cloud\webapp\upload.jsp"    "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\upload.jsp"
Copy-Item -Force "D:\j-cloud\webapp\files.jsp"     "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\files.jsp"

# Push %TOMCAT_HOME% jcloud folder into actual Tomcat installation
Copy-Item -Recurse -Force "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud" "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\"
```

---

### ✅ Option B — Deploy via `jcloud` folder in the repo

```powershell
# Copy compiled classes
Copy-Item -Recurse -Force "D:\j-cloud\bin\shared\*"  "D:\j-cloud\jcloud\WEB-INF\classes\shared\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\utils\*"   "D:\j-cloud\jcloud\WEB-INF\classes\utils\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\dao\*"     "D:\j-cloud\jcloud\WEB-INF\classes\dao\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\servlet\*" "D:\j-cloud\jcloud\WEB-INF\classes\servlet\"

# Copy all JSP files (including new files.jsp from Day 8)
Copy-Item -Force "D:\j-cloud\webapp\index.jsp"     "D:\j-cloud\jcloud\index.jsp"
Copy-Item -Force "D:\j-cloud\webapp\login.jsp"     "D:\j-cloud\jcloud\login.jsp"
Copy-Item -Force "D:\j-cloud\webapp\register.jsp"  "D:\j-cloud\jcloud\register.jsp"
Copy-Item -Force "D:\j-cloud\webapp\dashboard.jsp" "D:\j-cloud\jcloud\dashboard.jsp"
Copy-Item -Force "D:\j-cloud\webapp\upload.jsp"    "D:\j-cloud\jcloud\upload.jsp"
Copy-Item -Force "D:\j-cloud\webapp\files.jsp"     "D:\j-cloud\jcloud\files.jsp"

# Push jcloud folder into actual Tomcat installation
Copy-Item -Recurse -Force "D:\j-cloud\jcloud" "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\"
```

---

### ✅ Option C — Deploy directly to Tomcat installation

```powershell
# Copy compiled classes directly to Tomcat
Copy-Item -Recurse -Force "D:\j-cloud\bin\shared\*"  "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\WEB-INF\classes\shared\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\utils\*"   "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\WEB-INF\classes\utils\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\dao\*"     "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\WEB-INF\classes\dao\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\servlet\*" "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\WEB-INF\classes\servlet\"

# Copy JDBC driver and web.xml directly to Tomcat
Copy-Item -Force "D:\j-cloud\webapp\WEB-INF\lib\postgresql-42.7.10.jar" "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\WEB-INF\lib\"
Copy-Item -Force "D:\j-cloud\webapp\WEB-INF\web.xml"                    "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\WEB-INF\web.xml"

# Copy all JSP files directly to Tomcat (including new files.jsp from Day 8)
Copy-Item -Force "D:\j-cloud\webapp\index.jsp"     "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\index.jsp"
Copy-Item -Force "D:\j-cloud\webapp\login.jsp"     "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\login.jsp"
Copy-Item -Force "D:\j-cloud\webapp\register.jsp"  "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\register.jsp"
Copy-Item -Force "D:\j-cloud\webapp\dashboard.jsp" "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\dashboard.jsp"
Copy-Item -Force "D:\j-cloud\webapp\upload.jsp"    "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\upload.jsp"
Copy-Item -Force "D:\j-cloud\webapp\files.jsp"     "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\files.jsp"
```

---

### Restart Tomcat after any deploy (required for DownloadServlet to load)

```powershell
& "C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\shutdown.bat"
Start-Sleep -Seconds 4
& "C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\startup.bat"
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

### Test 3: Test Web Application — Upload

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

### Test 4: Test Web Application — Download (Day 8)

1. From Dashboard, click **Download File** or **My Files**
2. The **My Files** page (`/files.jsp`) opens — lists all your uploaded files
3. Click the **Download** button next to any file
4. Browser triggers a file download
5. Open the downloaded file — verify it is not corrupted and matches the original

---

## 📊 System Architecture
```
┌─────────────────────────────────────────────────────┐
│              TOMCAT (port 8080)                      │
│  Register  Login  Dashboard  Upload   Download       │
│  Servlet   Servlet  JSP      Servlet  Servlet        │
└──────────────────┬──────────────────────────────────┘
                   │
         ┌─────────▼──────────┐
         │  DAOs              │
         │  UserDAO NodeDAO   │
         │  FileDAO ChunkDAO  │
         │  ChunkLocationDAO  │
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
- Verify `UploadServlet.class` exists in `C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\WEB-INF\classes\servlet\`

### Issue: "Tomcat 404 error on /download"
- Verify `DownloadServlet.class` exists in `C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\WEB-INF\classes\servlet\`
- Restart Tomcat — new servlet classes require a restart to register

### Issue: "Download starts but file is corrupted or empty"
- Ensure DataNode(s) are running — chunk bytes cannot be fetched if nodes are DEAD
- Confirm rows exist in `chunk_locations` table for the file

### Issue: "files.jsp shows 404"
- Ensure `files.jsp` was copied into the deployment folder and into Tomcat webapps
- Re-run the deploy commands from Step 4

### Issue: "compile.bat skips servlet compilation"
Ensure `servlet-api.jar` exists in project root `D:\j-cloud\servlet-api.jar`

---

## 📈 Next Steps

Now that upload and download are both working, you can implement:

1. ✅ File Upload — chunking and distribution
2. ✅ File Download — chunk stitching and streaming
3. ✅ My Files page — list and download uploaded files
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
✅ Dashboard shows live file count, node status, and Recent Files table
✅ **Download File** and **My Files** buttons on dashboard are ACTIVE (purple, not grey)
✅ Upload File page works at `http://localhost:8080/jcloud/upload`
✅ After upload — chunk `.dat` files appear in `D:\j-cloud\storage\`
✅ After upload — rows in `files`, `chunks`, `chunk_locations` tables
✅ My Files page at `http://localhost:8080/jcloud/files.jsp` lists all files
✅ Clicking Download on any file triggers a browser download
✅ Downloaded file opens correctly and matches the original

---

## 💡 Development Tips

- **Use separate terminals** for each component (easier debugging)
- **Check logs** in each terminal for errors
- **Always start in order** — Master → DataNodes → Tomcat → Browser
- **Never upload before starting DataNodes** — you will get "No active nodes" error
- **Never download before starting DataNodes** — chunk retrieval will fail
- **Database first** — always verify DB before starting nodes
- **Monitor heartbeats** — key indicator of system health
- **Tomcat restart needed** after deploying new servlet `.class` files
- **Three deploy paths available** — `%TOMCAT_HOME%`, `jcloud` folder, or direct to Tomcat installation — all produce the same result
- **PowerShell tip** — the folder is literally named `%TOMCAT_HOME%`, use it directly in paths

---

Built with ❤️ using scalable Java patterns: Thread Pools, Connection Pooling, Scheduled Executors, Round-Robin Distribution, Chunk Stitching, and Concurrent Data Structures.