# ☁️ J-Cloud - Distributed File Storage System

J-Cloud is a production-ready distributed object storage system inspired by HDFS and AWS S3, built with scalable Java patterns including thread pools, connection pooling, and asynchronous scheduled tasks.

## 🎯 Project Status: **ALL 4 PHASES COMPLETE ✅**

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

## 🚀 Features

- ✅ **Distributed Architecture** - Master-Worker pattern with multiple data nodes
- ✅ **Thread Pool Management** - Scalable connection handling
- ✅ **Health Monitoring** - Automatic node death detection
- ✅ **Database Persistence** - Shared Neon PostgreSQL storage
- ✅ **User Authentication** - Secure login/registration system
- ✅ **Web Interface** - Modern JSP-based UI
- ⏳ **File Chunking** - (Coming next)
- ⏳ **Replication** - (Coming next)
- ⏳ **Load Balancing** - (Coming next)

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                  WEB LAYER (8080)                    │
│   Register → Login → Dashboard                       │
│   (Servlets + JSP + Session Management)              │
└─────────────────┬───────────────────────────────────┘
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
- **Server:** Apache Tomcat 9.0
- **Patterns:** Singleton, DAO, MVC, Thread Pool

## 🔧 Configuration

### System Endpoints
- **Master Node:** `localhost:9000`
- **Data Node 1:** `localhost:9101`
- **Data Node 2:** `localhost:9102`
- **Database:** Neon PostgreSQL (`.env` -> `JCLOUD_DB_URL`)
- **Tomcat Server:** `localhost:8080`


## 📦 Quick Start

### 1. Compile the Project
```cmd
compile.bat
```

### 2. Start Master Node
```cmd
run-master.bat
```

### 3. Start Data Nodes
Open separate terminals:
```cmd
run-datanode1.bat
run-datanode2.bat
```

### 4. Deploy to Tomcat
- Copy `webapp/` contents to Tomcat's `webapps/jcloud/`
- Copy compiled classes to `WEB-INF/classes/`
- Start Tomcat

### 5. Access Web Interface
Open browser: `http://localhost:8080/jcloud`

## 🧪 Testing

Run database tests:
```cmd
test-database.bat
```

Test heartbeat system:
1. Start Master
2. Start Data Node 1
3. Watch heartbeat messages
4. Kill Data Node (Ctrl+C)
5. Wait 15 seconds - Master marks it DEAD

## 📚 Documentation

- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Complete setup and deployment instructions
- **[database/schema.sql](database/schema.sql)** - Database schema
- **Protocol Format:** Pipe-delimited strings (`REGISTER_NODE|name|ip|port|capacity`)

## 🔑 Key Design Patterns

1. **Singleton Pattern** - `DBConnection.java` prevents connection pool exhaustion
2. **DAO Pattern** - `UserDAO.java`, `NodeDAO.java` abstract database operations
3. **Thread Pool Pattern** - `ExecutorService` for scalable client handling
4. **Scheduled Tasks** - `ScheduledExecutorService` for heartbeats and monitoring
5. **Concurrent Collections** - `ConcurrentHashMap` for thread-safe node tracking

## 📁 Project Structure

```
J-Cloud/
├── shared/              # Shared data models (POJOs)
├── utils/               # DBConnection singleton
├── dao/                 # Data Access Objects
├── master/              # Master Node server
├── datanode/            # Data Node servers
├── webapp/              # Web application
│   ├── servlet/         # Servlets (Register, Login, Logout)
│   ├── WEB-INF/         # Web config
│   └── *.jsp            # JSP pages
├── database/            # SQL schema
├── *.bat                # Helper scripts
└── DEPLOYMENT_GUIDE.md  # Detailed setup guide
```

## 🎯 Next Steps

1. ✅ Core infrastructure complete
2. ⏳ Implement file upload/download
3. ⏳ Add chunking algorithm
4. ⏳ Implement replication logic
5. ⏳ Build file management UI
6. ⏳ Add load balancing

## 👥 Team Configuration

- **Database Setup:** Shared by team member
- **Backend Development:** Scalable Java implementation
- **Web Interface:** Modern JSP/Servlet architecture


Educational project - J-Cloud Distributed File Storage System

