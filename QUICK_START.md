# 🚀 Quick Start Guide - J-Cloud (Neon PostgreSQL)

## Get J-Cloud running with Neon + Tomcat

---

## Prerequisites Check ✅

Before starting, verify:
```powershell
java -version
```
Expected: Java 8+.

Also verify these files exist:
- `D:\j-cloud\.env`
- `D:\j-cloud\servlet-api.jar`
- `D:\j-cloud\webapp\WEB-INF\lib\postgresql-42.7.10.jar`

---

## Step 1: Configure Database URL in `.env`

Project root file `.env` must contain your Neon URL:
```text
JCLOUD_DB_URL=postgresql://<user>:<password>@<host>/<database>?sslmode=require&channel_binding=require
```

Do not use localhost DB settings.

---

## Step 2: Compile Project
```powershell
compile.bat
```

Wait for:
```
=====================================
  COMPILATION SUCCESSFUL!
=====================================
```

`compile.bat` handles everything — shared, utils, dao, master, datanode, and all servlet classes.

---

## Step 3: Verify DB + Phase 1 quickly
```powershell
test-database.bat
```

Expected outcomes:
- Database connection successful
- User registration/authentication successful
- Node registration + fetch successful

---

## Step 4: Deploy to `%TOMCAT_HOME%`

Copy compiled classes and JSPs into the `%TOMCAT_HOME%` folder already in the repo:
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

## Step 5: Start Services (4 terminals)

### Terminal 1
```powershell
cd D:\j-cloud
./run-master.bat
```
Wait for: `✓ Heartbeat monitor started`

### Terminal 2
```powershell
cd D:\j-cloud
./run-datanode1.bat
```
Wait for: `✓ Registration successful!` and `♥ Heartbeat sent and acknowledged`

### Terminal 3
```powershell
cd D:\j-cloud
./run-datanode2.bat
```
Wait for: `✓ Registration successful!`

### Terminal 4
```powershell
& "C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\startup.bat"
```

---

## Step 6: Validate Web Flow

Open:
```
http://localhost:8080/jcloud
```

Validate:
- Register user
- Login user
- Dashboard opens — shows active nodes and file count
- Click Upload File
- Select any file and click Upload & Distribute
- Check DataNode terminals for chunk confirmation
- Verify in DB:
```sql
SELECT * FROM files;
SELECT * FROM chunks;
SELECT * FROM chunk_locations;
```

---

## Troubleshooting

### Problem: DB connection fails
Cause: `.env` missing or wrong `JCLOUD_DB_URL`.
Fix: Update `.env` and rerun `test-database.bat`.

### Problem: PostgreSQL driver not found during compile
Ensure `webapp\WEB-INF\lib\postgresql-42.7.10.jar` exists.

### Problem: Servlet compilation skipped in compile.bat
Ensure `servlet-api.jar` exists in project root `D:\j-cloud\`.

### Problem: Port 9000 already in use
```powershell
taskkill /IM java.exe /F
```

### Problem: Upload fails — no active data nodes
Start `run-datanode1.bat` before uploading.

### Problem: Tomcat 404 on /upload
Re-run Step 4 deploy commands.

### Problem: Tomcat app opens but login/register fails
Redeploy — confirm updated classes are in `%TOMCAT_HOME%\webapps\jcloud\WEB-INF\classes\`.

---

## Day 5 Success Checklist

- Master running on `9000`
- DataNode1 and DataNode2 sending heartbeats
- `test-database.bat` completes successfully
- Tomcat serves `http://localhost:8080/jcloud`
- Register/Login works on web UI
- Dashboard shows active node count and file count
- Upload File page opens at `http://localhost:8080/jcloud/upload`
- After upload — `.dat` chunk files appear in `D:\j-cloud\storage\`
- After upload — rows appear in `files`, `chunks`, `chunk_locations` tables

If all are true, Day 5 is working as intended with Neon PostgreSQL.