<img width="542" height="661" alt="image" src="https://github.com/user-attachments/assets/9717bc51-e6cb-409d-a374-91fa30b9657a" />
**Created by [Dhriti Gandhi](https://github.com/Dhriti-5) & [Disha Vaghela](https://github.com/DishaVaghela906)**
J-Cloud is a production-ready, distributed object storage system inspired by HDFS and AWS S3. Built entirely in Java, it demonstrates enterprise-level system design, combining synchronous user-facing operations with autonomous, asynchronous background recovery and replication.

The system utilizes a **Master-DataNode architecture** to solve traditional single-server constraints, offering unlimited horizontal scalability, fault tolerance, and high availability through file sharding and automated self-healing.

---

## 🚀 Key Features

*   **Distributed Architecture:** Master-Worker topology utilizing a dual-plane socket protocol (Control Plane + Data Plane).
*   **File Sharding & Reassembly:** Large files are automatically split into 5 MB chunks, distributed across multiple DataNodes, and seamlessly reassembled in sequence during downloads.
*   **Self-Healing & Replication:** An asynchronous `ReplicationManager` continuously scans for under-replicated chunks and rebuilds them on healthy nodes if a DataNode fails.
*   **Efficient Deletion:** Metadata-first deletion strategy ensures immediate UI response for users, while physical chunk cleanup happens asynchronously in the background.
*   **Concurrency:** Robust thread pool management in both the Master and DataNodes to handle simultaneous socket requests and parallel chunk uploads.
*   **Admin Dashboard:** Real-time visibility into cluster health, node capacity, active vs. dead nodes, and a system event audit log.

---

## 🏗️ High-Level Architecture

```text
┌──────────────────────────────────────────────────────────────┐
│                      WEB LAYER (Tomcat)                      │
│   Register → Login → File Dashboard → Admin Monitoring       │
│   (Servlets + JSP + Session Management + DataNodeClient)     │
└──────┬───────────────────────┬───────────────────────┬───────┘
       │ Control flow          │ Data flow             │
┌──────▼──────┐         ┌──────▼──────┐         ┌──────▼──────┐
│ MASTER NODE │         │ DATA NODE 1 │         │ DATA NODE 2 │
│  (Port 9000)│         │ (Port 9101) │         │ (Port 9102) │
│             │         │             │         │             │
│ - Node Reg  │         │ - Chunk I/O │         │ - Chunk I/O │
│ - Heartbeat ◄─────────┤ - Heartbeat │         │ - Heartbeat │
│ - Healing   │         │ - Storage   │         │ - Storage   │
└──────┬──────┘         └──────┬──────┘         └──────┬──────┘
       │ Metadata              │ Physical Files        │ Physical Files
┌──────▼───────────────────────▼───────────────────────▼──────┐
│                     PERSISTENCE LAYER                       │
│    PostgreSQL Database (Users, Files, Chunks, Nodes, Logs)  │
└─────────────────────────────────────────────────────────────┘
```

---

## 💻 Tech Stack

*   **Core Backend:** Java 8+ (Socket Programming, Multithreading, `ExecutorService`, `ScheduledExecutorService`)
*   **Web Layer:** Servlets 4.0, JSP, HTML/CSS
*   **Database:** PostgreSQL (Neon Cloud) via JDBC
*   **Server:** Apache Tomcat 9.0
*   **Design Patterns:** Singleton, DAO, MVC, Thread Pool, Master-Worker

---

## 🔌 Core Protocols

J-Cloud operates on a custom dual-plane socket protocol:

**1. Master Control Protocol (Port 9000)**
*   `REGISTER_NODE|nodeName|ip|port|capacity`
*   `HEARTBEAT|nodeName`
*   `DELETE_REQUEST|fileId`

**2. DataNode Chunk Protocol (Ports 9101, 9102...)**
*   `STORE_CHUNK|chunkId|chunkIndex|fileId|chunkSize|fileName + <RAW BYTES>`
*   `GET_CHUNK|chunkId`
*   `DELETE_CHUNK|chunkId`

---

## 🗄️ Database Schema Overview

The system is metadata-driven, utilizing six primary tables:
1.  **`users`**: Credentials and profile data.
2.  **`files`**: Metadata for uploaded files (1:N with users).
3.  **`chunks`**: Metadata for file segments (1:N with files).
4.  **`nodes`**: Registry of active, dead, and available storage servers.
5.  **`chunk_locations`**: Mapping of chunks to physical node locations (N:M mapping).
6.  **`event_logs`**: System audit trail for failures, recoveries, and purges.

---

## 📦 Quick Start & Deployment

### Prerequisites
*   Java JDK 8+
*   Apache Tomcat 9
*   PostgreSQL Database (Create `.env` file with `JCLOUD_DB_URL` and admin credentials)

### 1. Compile the Project
```cmd
compile.bat
```

### 2. Start the Cluster
Open separate terminal windows and run:
```cmd
run-master.bat
run-datanode1.bat
run-datanode2.bat
```

### 3. Deploy Web Application
*   Copy `webapp/` contents to Tomcat's `webapps/jcloud/`
*   Copy compiled `.class` files to `WEB-INF/classes/`
*   Start the Tomcat server.

### 4. Access the Platform
*   **User UI:** `http://localhost:8080/jcloud`
*   **Admin Dashboard:** Log in with configured environment admin credentials.

---

## 🧪 Testing System Resilience

To witness J-Cloud's self-healing capabilities in action:
1. Upload a large file (it will be chunked and replicated).
2. Kill one of the DataNode terminals (`Ctrl+C`).
3. Wait for the `HeartbeatMonitor` to time out.
4. Check the Admin Dashboard: The node will be marked **DEAD**, and `METADATA_PURGE` will appear in the event logs.
5. Wait for the next `ReplicationManager` cycle: It will detect under-replicated chunks and automatically copy them to surviving nodes to restore target replication factors.
