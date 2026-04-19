# J-Cloud Full Clean Redeployment Guide (All Features)

This guide is for full project redeployment.
It validates all core features end-to-end:

- Auth (Register/Login/Logout)
- Upload and chunking
- My Files listing
- Download
- Delete
- Replication
- Failure detection
- Auto-recovery and event logs

## 1) Team Deployment Model

- Machine A (teammate): Tomcat + Master + DataNode2 + DataNode3 (temporary)
- Machine B (you): DataNode1
- Primary admin page: http://100.89.131.79:8080/jcloud/admin.jsp

## 2) Pre-Deployment Requirements

On both machines:

- Java installed and available in PATH
- Same git branch checked out
- Access to Neon PostgreSQL
- Tailscale connectivity between both machines

On Machine A:

- Tomcat installed at:
  C:\Program Files\Apache Software Foundation\Tomcat 9.0

## 3) Database Preparation (Neon)

Run once (safe idempotent form):

```sql
CREATE TABLE IF NOT EXISTS event_logs (
    log_id SERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Optional checks:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM files;
SELECT COUNT(*) FROM chunks;
SELECT COUNT(*) FROM chunk_locations;
SELECT COUNT(*) FROM nodes;
SELECT COUNT(*) FROM event_logs;
```

Optional clean test reset (use only if you want a fresh demo dataset):

```sql
TRUNCATE TABLE chunk_locations RESTART IDENTITY;
TRUNCATE TABLE chunks RESTART IDENTITY;
TRUNCATE TABLE files RESTART IDENTITY;
TRUNCATE TABLE event_logs RESTART IDENTITY;
TRUNCATE TABLE nodes RESTART IDENTITY;
```

## 4) Git Sync + Build (Both Machines)

Run on Machine A and Machine B:

```powershell
cd C:\Users\Pc\J-Cloud
git fetch
git checkout pr-jcloud
git pull origin pr-jcloud
.\compile.bat
```

If your branch name is different, replace it in both checkout/pull commands.

## 5) Machine A Clean Tomcat Redeploy (Full)

Run this block in Administrator PowerShell on Machine A:

```powershell
$Repo = 'C:\Users\Pc\J-Cloud'
$Tomcat = 'C:\Program Files\Apache Software Foundation\Tomcat 9.0'
$AppName = 'jcloud'
$WebAppTarget = Join-Path $Tomcat "webapps\$AppName"
$WorkTarget = Join-Path $Tomcat "work\Catalina\localhost\$AppName"

# Stop Tomcat
& (Join-Path $Tomcat 'bin\shutdown.bat')
Start-Sleep -Seconds 5

# Remove old app and compiled JSP cache
if (Test-Path $WebAppTarget) { Remove-Item -Recurse -Force $WebAppTarget }
if (Test-Path $WorkTarget)   { Remove-Item -Recurse -Force $WorkTarget }

# Recreate app directories
New-Item -ItemType Directory -Force -Path "$WebAppTarget\WEB-INF\classes\shared"   | Out-Null
New-Item -ItemType Directory -Force -Path "$WebAppTarget\WEB-INF\classes\utils"    | Out-Null
New-Item -ItemType Directory -Force -Path "$WebAppTarget\WEB-INF\classes\dao"      | Out-Null
New-Item -ItemType Directory -Force -Path "$WebAppTarget\WEB-INF\classes\servlet"  | Out-Null
New-Item -ItemType Directory -Force -Path "$WebAppTarget\WEB-INF\classes\master"   | Out-Null
New-Item -ItemType Directory -Force -Path "$WebAppTarget\WEB-INF\classes\datanode" | Out-Null
New-Item -ItemType Directory -Force -Path "$WebAppTarget\WEB-INF\lib"               | Out-Null

# Copy compiled classes
Copy-Item -Recurse -Force "$Repo\bin\shared\*"   "$WebAppTarget\WEB-INF\classes\shared\"
Copy-Item -Recurse -Force "$Repo\bin\utils\*"    "$WebAppTarget\WEB-INF\classes\utils\"
Copy-Item -Recurse -Force "$Repo\bin\dao\*"      "$WebAppTarget\WEB-INF\classes\dao\"
Copy-Item -Recurse -Force "$Repo\bin\servlet\*"  "$WebAppTarget\WEB-INF\classes\servlet\"
if (Test-Path "$Repo\bin\master")   { Copy-Item -Recurse -Force "$Repo\bin\master\*"   "$WebAppTarget\WEB-INF\classes\master\" }
if (Test-Path "$Repo\bin\datanode") { Copy-Item -Recurse -Force "$Repo\bin\datanode\*" "$WebAppTarget\WEB-INF\classes\datanode\" }

# Copy web.xml and JDBC driver
Copy-Item -Force "$Repo\webapp\WEB-INF\web.xml" "$WebAppTarget\WEB-INF\web.xml"
Copy-Item -Force "$Repo\webapp\WEB-INF\lib\postgresql-42.7.10.jar" "$WebAppTarget\WEB-INF\lib\"

# Copy all JSP pages
Copy-Item -Force "$Repo\webapp\*.jsp" "$WebAppTarget\"

# Start Tomcat
& (Join-Path $Tomcat 'bin\startup.bat')
Write-Host 'Tomcat full clean redeploy complete.' -ForegroundColor Green
```

## 6) Start Runtime Services

### 6.1 Machine A (teammate)

Open three terminals in C:\Users\Pc\J-Cloud:

Terminal A1:

```powershell
.\run-master.bat
```

Terminal A2:

```powershell
.\run-datanode2.bat
```

Terminal A3 (DataNode3 startup command):

```powershell
cd C:\Users\Pc\J-Cloud
$JDBC = (Get-ChildItem "$PWD\postgresql-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
if (-not $JDBC) { $JDBC = (Get-ChildItem "$PWD\webapp\WEB-INF\lib\postgresql-*.jar" | Select-Object -First 1).FullName }
java -cp "bin;$JDBC" datanode.DataNodeServer DataNode3 9103
```

### 6.2 Machine B (you)

```powershell
cd C:\Users\Pc\J-Cloud
.\run-datanode1.bat
```

## 7) Full Feature Smoke Test (Do This In Order)

### 7.1 App Reachability

- Open: http://100.89.131.79:8080/jcloud
- Open: http://100.89.131.79:8080/jcloud/dashboard.jsp
- Open: http://100.89.131.79:8080/jcloud/files.jsp
- Open: http://100.89.131.79:8080/jcloud/admin.jsp

### 7.2 Auth Flow (Register/Login/Logout)

1. Register a brand new user.
2. Login with same user.
3. Confirm dashboard loads with user context.
4. Logout and confirm session clears.

### 7.3 Upload + Chunking

1. Login again.
2. Upload a medium file.
3. Confirm upload success in UI.
4. Confirm file appears in My Files.

Verify in SQL:

```sql
SELECT file_id, file_name, file_size, owner_id, upload_time
FROM files
ORDER BY file_id DESC
LIMIT 5;

SELECT chunk_id, file_id, chunk_index, chunk_size
FROM chunks
ORDER BY chunk_id DESC
LIMIT 20;

SELECT id, chunk_id, node_id
FROM chunk_locations
ORDER BY id DESC
LIMIT 40;
```

### 7.4 Download

1. Click Download for uploaded file.
2. Open downloaded file and verify integrity.

### 7.5 Delete

1. Delete the uploaded file from UI.
2. Confirm file disappears from My Files.

Verify metadata cleanup:

```sql
SELECT * FROM files ORDER BY file_id DESC LIMIT 10;
SELECT * FROM chunks ORDER BY chunk_id DESC LIMIT 20;
SELECT * FROM chunk_locations ORDER BY id DESC LIMIT 40;
```

### 7.6 Replication (Day 10)

1. Upload another file while DataNode1+2+3 are online.
2. Verify chunk_locations has multiple entries per chunk.

Quick RF view:

```sql
SELECT chunk_id, COUNT(DISTINCT node_id) AS replica_count
FROM chunk_locations
GROUP BY chunk_id
ORDER BY chunk_id DESC;
```

### 7.7 Failure Detection + Auto-Recovery (Days 11-12)

1. Keep Master + DataNode1 + DataNode2 + DataNode3 running.
2. Kill DataNode2 terminal.
3. Wait around 15 seconds:
   - Node status should become DEAD.
   - event_logs should include NODE_FAILURE and METADATA_PURGE.
4. Wait up to 60 seconds (ReplicationManager cycle):
   - Under-replicated chunks should be healed to healthy nodes.
   - event_logs should include recovery success entries.

Verification SQL:

```sql
SELECT node_id, node_name, ip_address, port, status
FROM nodes
ORDER BY node_id;

SELECT log_id, event_type, message, created_at
FROM event_logs
ORDER BY created_at DESC
LIMIT 30;

SELECT
    COUNT(DISTINCT CASE WHEN rep_count >= 2 THEN chunk_id END) AS healthy_chunks,
    COUNT(DISTINCT CASE WHEN rep_count < 2 THEN chunk_id END) AS under_replicated_chunks
FROM (
    SELECT chunk_id, COUNT(DISTINCT node_id) AS rep_count
    FROM chunk_locations
    GROUP BY chunk_id
) t;
```

## 8) Common Issues and Fixes

- Old JSP still appears:
  - Rerun Section 5 and ensure Tomcat work cache folder is deleted.
- 404 on servlet/JSP:
  - Confirm JSP files copied and servlet classes exist under WEB-INF/classes/servlet.
- DataNode3 cannot start:
  - Ensure Master is running first.
  - Ensure port 9103 is free.
  - Ensure JDBC jar was detected in command.
- DataNode1 cannot register:
  - Verify network path to Master over Tailscale and master host/port config.
- No recovery observed:
  - Confirm dead-node chunk_locations were purged.
  - Wait full 60-second replication cycle.

## 9) Final Sign-Off Checklist

- Database migration done (event_logs exists)
- Both machines pulled latest code
- compile.bat succeeded on both machines
- Machine A running Tomcat + Master + DataNode2 + DataNode3
- Machine B running DataNode1
- Register/Login/Logout works
- Upload/Files/Download/Delete works
- Replication verified in chunk_locations
- Failure detection verified in nodes table
- Auto-recovery verified via event_logs and replica_count
