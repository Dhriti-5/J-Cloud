# ☁️ J-Cloud - Distributed File Storage System

J-Cloud is a production-ready distributed object storage system inspired by HDFS and AWS S3, built with scalable Java patterns including thread pools, connection pooling, and asynchronous scheduled tasks.

## 🎯 Project Status: ALL 8 DAYS COMPLETE ✅

### ✅ Phase 1: Database & Shared Layer
- Singleton pattern for PostgreSQL connection pooling
- UserDAO and NodeDAO with prepared statements
- Shared POJOs with proper encapsulation

### ✅ Phase 2: Scalable Master Node
- Thread pool executor for 50 concurrent connections
- Protocol parser for REGISTER_NODE and HEARTBEAT
- Automated heartbeat monitor with 15-second timeout
- ConcurrentHashMap for thread-safe node tracking

### ✅ Phase 3: Data Node Development
- Auto-registration with Master on startup
- ScheduledExecutorService for 5-second heartbeat intervals
- Graceful shutdown with resource cleanup
- Support for multiple data nodes

### ✅ Phase 4: Tomcat Web Application
- User registration with MD5 password hashing
- Session-based authentication system
- Beautiful responsive JSP pages
- RESTful servlet architecture

### ✅ Phase 5: File Upload (Day 5)
- Chunked file upload — 5 MB per chunk
- Parallel upload using Java ExecutorService thread pool
- Round-robin chunk distribution across active data nodes
- Chunk metadata stored in PostgreSQL (files, chunks, chunk_locations)
- Drag and drop upload UI with progress bar
- Dashboard shows live file count, storage used, active nodes

### ✅ Phase 6: File Download System (Day 8)
- Distributed chunk retrieval in correct sequence order
- Chunks fetched from data nodes via TCP socket and streamed directly to the browser
- My Files page listing all uploaded files with size, chunk count, and download button
- Dashboard updated with active Download File and My Files buttons
- Recent Files table with inline download links shown directly on dashboard
- Fault-tolerant download — if a node is DEAD, next available replica is tried automatically

## 🚀 Features

- ✅ **Distributed Architecture** - Master-Worker pattern with multiple data nodes
- ✅ **Thread Pool Management** - Scalable connection handling
- ✅ **Health Monitoring** - Automatic node death detection
- ✅ **Database Persistence** - Shared Neon PostgreSQL storage
- ✅ **User Authentication** - Secure login/registration system
- ✅ **Web Interface** - Modern JSP-based UI
- ✅ **File Upload** - Chunked parallel distributed upload
- ✅ **File Download** - Chunk stitching and seamless streaming to browser
- ✅ **My Files Page** - List, browse, and download all uploaded files
- ⏳ **Replication** - (Coming next)
- ⏳ **Load Balancing** - (Coming next)

## 🏗️ Architecture
```
┌─────────────────────────────────────────────────────┐
│              WEB LAYER (port 8080)                   │
│   Register → Login → Dashboard → Upload → Download  │
│   (Servlets + JSP + Session Management)              │
└─────────────────┬───────────────────────────────────┘
                  │
         ┌────────▼─────────┐
         │   DAOs           │
         │ UserDAO NodeDAO  │
         │ FileDAO ChunkDAO │
         │ ChunkLocationDAO │
         └────────┬─────────┘
                  │
         ┌────────▼──────────┐
         │   DBConnection    │
         │   (Singleton)     │
         └────────┬──────────┘
                  │
         ┌────────▼──────────┐
         │ Neon PostgreSQL   │
         │  (Shared Cloud)   │
         └───────────────────┘

┌────────────────────────────────────────────────────┐
│         MASTER NODE (9000)                         │
│   Thread Pool → Client Handler → Heartbeat Monitor │
│   ConcurrentHashMap for Node Tracking              │
└───────────┬────────────────────┬──────────────────┘
            │                    │
    ┌───────▼──────┐     ┌──────▼──────┐
    │ Data Node 1  │     │ Data Node 2 │
    │ Port: 9101   │     │ Port: 9102  │
    │ Heartbeat 5s │     │ Heartbeat 5s│
    │ storage/     │     │ storage/    │
    └──────────────┘     └─────────────┘
```

## 💻 Tech Stack

- **Backend:** Java 8+ with Sockets, Thread Pools, Scheduled Executors
- **Web Layer:** Servlets 4.0, JSP, Session Management
- **Database:** PostgreSQL (Neon) with JDBC
- **Server:** Apache Tomcat 9.0 (inside `%TOMCAT_HOME%` folder in repo)
- **Patterns:** Singleton, DAO, MVC, Thread Pool, Round-Robin

## 🔧 Configuration

### System Endpoints
- **Master Node:** `localhost:9000`
- **Data Node 1:** `localhost:9101`
- **Data Node 2:** `localhost:9102`
- **Database:** Neon PostgreSQL (`.env` -> `JCLOUD_DB_URL`)
- **Tomcat Server:** `localhost:8080`

## 📦 Quick Start

### 1. Configure Database
Create `.env` in project root:
```
JCLOUD_DB_URL=postgresql://user:password@host/neondb?sslmode=require&channel_binding=require
```

### 2. Compile the Project
```powershell
cd D:\j-cloud
compile.bat
```

### 3. Deploy to Tomcat

**Option A — via `%TOMCAT_HOME%` folder in the repo:**
```powershell
Copy-Item -Recurse -Force "D:\j-cloud\bin\shared\*"  "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\shared\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\utils\*"   "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\utils\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\dao\*"     "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\dao\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\servlet\*" "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\servlet\"
Copy-Item -Force "D:\j-cloud\webapp\WEB-INF\lib\postgresql-42.7.10.jar" "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\lib\"
Copy-Item -Force "D:\j-cloud\webapp\WEB-INF\web.xml"                    "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\web.xml"
Copy-Item -Force "D:\j-cloud\webapp\index.jsp"     "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\index.jsp"
Copy-Item -Force "D:\j-cloud\webapp\login.jsp"     "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\login.jsp"
Copy-Item -Force "D:\j-cloud\webapp\register.jsp"  "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\register.jsp"
Copy-Item -Force "D:\j-cloud\webapp\dashboard.jsp" "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\dashboard.jsp"
Copy-Item -Force "D:\j-cloud\webapp\upload.jsp"    "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\upload.jsp"
Copy-Item -Force "D:\j-cloud\webapp\files.jsp"     "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\files.jsp"
Copy-Item -Recurse -Force "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud" "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\"
```

**Option B — via `jcloud` folder in the repo:**
```powershell
Copy-Item -Recurse -Force "D:\j-cloud\bin\shared\*"  "D:\j-cloud\jcloud\WEB-INF\classes\shared\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\utils\*"   "D:\j-cloud\jcloud\WEB-INF\classes\utils\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\dao\*"     "D:\j-cloud\jcloud\WEB-INF\classes\dao\"
Copy-Item -Recurse -Force "D:\j-cloud\bin\servlet\*" "D:\j-cloud\jcloud\WEB-INF\classes\servlet\"
Copy-Item -Force "D:\j-cloud\webapp\dashboard.jsp" "D:\j-cloud\jcloud\dashboard.jsp"
Copy-Item -Force "D:\j-cloud\webapp\files.jsp"     "D:\j-cloud\jcloud\files.jsp"
Copy-Item -Force "D:\j-cloud\webapp\upload.jsp"    "D:\j-cloud\jcloud\upload.jsp"
Copy-Item -Force "D:\j-cloud\webapp\login.jsp"     "D:\j-cloud\jcloud\login.jsp"
Copy-Item -Force "D:\j-cloud\webapp\register.jsp"  "D:\j-cloud\jcloud\register.jsp"
Copy-Item -Force "D:\j-cloud\webapp\index.jsp"     "D:\j-cloud\jcloud\index.jsp"
Copy-Item -Recurse -Force "D:\j-cloud\jcloud" "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\"
```

**Restart Tomcat after deploying:**
```powershell
& "C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\shutdown.bat"
Start-Sleep -Seconds 4
& "C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\startup.bat"
```

### 4. Start Master Node
```powershell
./run-master.bat
```

### 5. Start Data Nodes
Open separate terminals:
```powershell
./run-datanode1.bat
./run-datanode2.bat
```

### 6. Access Web Interface
Open browser:
```
http://localhost:8080/jcloud
```

## 🧪 Testing

Run database tests:
```powershell
test-database.bat
```

Test heartbeat system:
1. Start Master
2. Start Data Node 1
3. Watch heartbeat messages
4. Kill Data Node (Ctrl+C)
5. Wait 15 seconds — Master marks it DEAD

Test upload:
1. Login to `http://localhost:8080/jcloud`
2. Click Upload File
3. Select any file and click Upload & Distribute
4. Check DataNode terminals for chunk storage confirmation

Test download (Day 8):
1. Login to `http://localhost:8080/jcloud`
2. Click **My Files** or **Download File** from dashboard
3. Click **Download** next to any uploaded file
4. Browser downloads the reassembled file — open it to verify

## 🔑 Key Design Patterns

1. **Singleton Pattern** - `DBConnection.java` prevents connection pool exhaustion
2. **DAO Pattern** - `UserDAO.java`, `NodeDAO.java`, `FileDAO.java`, `ChunkDAO.java`, `ChunkLocationDAO.java`
3. **Thread Pool Pattern** - `ExecutorService` for parallel uploads and client handling
4. **Round-Robin** - Chunk distribution across data nodes
5. **Scheduled Tasks** - `ScheduledExecutorService` for heartbeats and monitoring
6. **Concurrent Collections** - `ConcurrentHashMap` for thread-safe node tracking
7. **Chunk Stitching** - Sequential chunk retrieval and streaming in DownloadServlet

## 📁 Project Structure
```
J-Cloud/
├── shared/              # Shared data models (POJOs)
├── utils/               # DBConnection singleton
├── dao/                 # Data Access Objects
├── master/              # Master Node server
├── datanode/            # Data Node servers
├── webapp/              # Web application (edit files here)
│   ├── servlet/         # Servlets (Login, Logout, Register, Upload, Download)
│   ├── WEB-INF/         # Web config + lib
│   └── *.jsp            # JSP pages (dashboard, files, upload, login, register)
├── %TOMCAT_HOME%/       # Tomcat deployment folder (already in repo)
│   └── webapps/jcloud/
├── jcloud/              # Alternative deployment folder (already in repo)
│   └── webapps/jcloud/
├── database/            # SQL schema
├── storage/             # Chunk .dat files stored by data nodes
├── bin/                 # Compiled .class files
├── servlet-api.jar      # Servlet API for compilation
├── compile.bat          # Compile all Java files
├── run-master.bat
├── run-datanode1.bat
├── run-datanode2.bat
├── test-database.bat
└── .env                 # DB credentials (gitignored)
```

## 🎯 Day-by-Day Progress

| Day | Feature | Status |
|-----|---------|--------|
| Day 1 | Architecture & Setup | ✅ |
| Day 2 | Database Schema & DAOs | ✅ |
| Day 3 | Master Node Skeleton | ✅ |
| Day 4 | Data Node + Heartbeat | ✅ |
| Day 5 | File Upload + Chunking | ✅ |
| Day 6 | Web UI + Authentication | ✅ |
| Day 7 | Metadata Storage | ✅ |
| Day 8 | File Download System | ✅ |

## 🎯 Next Steps

1. ✅ Core infrastructure complete
2. ✅ File upload with chunking implemented
3. ✅ File download with chunk stitching implemented
4. ✅ My Files page built
5. ⏳ Implement replication logic
6. ⏳ Add load balancing

## 👥 Team Configuration

- **Disha (Developer A)** — Web layer, Servlets, JSP, JDBC, Upload feature, Download feature, Deployment
- **Developer B** — Master Node, Data Nodes, Socket programming, Heartbeat

Educational project - J-Cloud Distributed File Storage System