# ☁️ J-Cloud - Distributed File Storage System

J-Cloud is a production-ready distributed object storage system inspired by HDFS and AWS S3, built with scalable Java patterns including thread pools, connection pooling, and asynchronous scheduled tasks.

## 🎯 Project Status: ALL 5 PHASES COMPLETE ✅

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

## 🚀 Features

- ✅ **Distributed Architecture** - Master-Worker pattern with multiple data nodes
- ✅ **Thread Pool Management** - Scalable connection handling
- ✅ **Health Monitoring** - Automatic node death detection
- ✅ **Database Persistence** - Shared Neon PostgreSQL storage
- ✅ **User Authentication** - Secure login/registration system
- ✅ **Web Interface** - Modern JSP-based UI
- ✅ **File Upload** - Chunked parallel distributed upload
- ⏳ **File Download** - (Coming in Phase 6)
- ⏳ **Replication** - (Coming next)
- ⏳ **Load Balancing** - (Coming next)

## 🏗️ Architecture
```
┌─────────────────────────────────────────────────────┐
│              WEB LAYER (port 8080)                   │
│   Register → Login → Dashboard → Upload             │
│   (Servlets + JSP + Session Management)              │
└─────────────────┬───────────────────────────────────┘
                  │
         ┌────────▼─────────┐
         │   DAOs           │
         │ UserDAO NodeDAO  │
         │ FileDAO ChunkDAO │
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
│   Thread Pool → Client Handler → Heartbeat Monitor│
│   ConcurrentHashMap for Node Tracking             │
└───────────┬────────────────────┬──────────────────┘
            │                    │
    ┌───────▼──────┐     ┌──────▼──────┐
    │ Data Node 1  │     │ Data Node 2 │
    │ Port: 9101   │     │ Port: 9102  │
    │ Heartbeat 5s │     │ Heartbeat 5s│
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
compile.bat
```

### 3. Deploy to Tomcat
```powershell
Copy-Item -Recurse -Force bin\shared\*   "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\shared\"
Copy-Item -Recurse -Force bin\utils\*    "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\utils\"
Copy-Item -Recurse -Force bin\dao\*      "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\dao\"
Copy-Item -Recurse -Force bin\servlet\*  "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\servlet\"
Copy-Item -Force webapp\*.jsp            "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\"
Copy-Item -Force webapp\WEB-INF\web.xml  "D:\j-cloud\%TOMCAT_HOME%\webapps\jcloud\WEB-INF\web.xml"
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

## 🔑 Key Design Patterns

1. **Singleton Pattern** - `DBConnection.java` prevents connection pool exhaustion
2. **DAO Pattern** - `UserDAO.java`, `NodeDAO.java`, `FileDAO.java`, `ChunkDAO.java`
3. **Thread Pool Pattern** - `ExecutorService` for parallel uploads and client handling
4. **Round-Robin** - Chunk distribution across data nodes
5. **Scheduled Tasks** - `ScheduledExecutorService` for heartbeats and monitoring
6. **Concurrent Collections** - `ConcurrentHashMap` for thread-safe node tracking

## 📁 Project Structure
```
J-Cloud/
├── shared/              # Shared data models (POJOs)
├── utils/               # DBConnection singleton
├── dao/                 # Data Access Objects
├── master/              # Master Node server
├── datanode/            # Data Node servers
├── webapp/              # Web application (edit files here)
│   ├── servlet/         # Servlets (Login, Logout, Register, Upload)
│   ├── WEB-INF/         # Web config + lib
│   └── *.jsp            # JSP pages
├── %TOMCAT_HOME%/       # Tomcat deployment folder (already in repo)
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

## 🎯 Next Steps

1. ✅ Core infrastructure complete
2. ✅ File upload with chunking implemented
3. ⏳ Implement file download (Phase 6)
4. ⏳ Build My Files page
5. ⏳ Implement replication logic
6. ⏳ Add load balancing

## 👥 Team Configuration

- **Disha (Developer A)** — Web layer, Servlets, JSP, JDBC, Upload feature, Deployment
- **Developer B** — Master Node, Data Nodes, Socket programming, Heartbeat

Educational project - J-Cloud Distributed File Storage System