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
cd D:\j-cloud
compile.bat
```

Wait for:
```
=====================================
  COMPILATION SUCCESSFUL!
=====================================
```

`compile.bat` handles everything — shared, utils, dao, master, datanode, and all servlet classes including the new `DownloadServlet.java` added in Day 8.

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

## Step 4: Deploy to Tomcat

Three deploy paths are available — use whichever applies to your setup. All three end with the same result: the `jcloud` app running in Tomcat at `http://localhost:8080/jcloud`.

---

### Option A — Deploy via `%TOMCAT_HOME%` folder in the repo

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

### Option B — Deploy via `jcloud` folder in the repo

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

### Option C — Deploy directly to Tomcat installation

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

## Step 5: Start Services (4 terminals)

### Terminal 1 — Master Node
```powershell
cd D:\j-cloud
./run-master.bat
```
Wait for: `✓ Heartbeat monitor started`

### Terminal 2 — Data Node 1
```powershell
cd D:\j-cloud
./run-datanode1.bat
```
Wait for: `✓ Registration successful!` and `♥ Heartbeat sent and acknowledged`

### Terminal 3 — Data Node 2
```powershell
cd D:\j-cloud
./run-datanode2.bat
```
Wait for: `✓ Registration successful!`

### Terminal 4 — Tomcat
```powershell
& "C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\startup.bat"
```

---

## Step 6: Validate Web Flow

Open:
```
http://localhost:8080/jcloud
```

### Upload Test
- Register user → Login → Dashboard
- Click **Upload File**
- Select any file → Click **Upload & Distribute**
- Check DataNode terminals for chunk confirmation
- Verify in DB:
```sql
SELECT * FROM files;
SELECT * FROM chunks;
SELECT * FROM chunk_locations;
```

### Download Test (Day 8)
- From Dashboard → Click **Download File** or **My Files**
- The **My Files** page (`/files.jsp`) shows all uploaded files with size, chunk count, and a Download button per file
- Click **Download** next to any file
- Browser downloads the reassembled file — open it to verify it is not corrupted

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

### Problem: Tomcat 404 on /upload or /download
Re-run Step 4 deploy commands. Confirm both `UploadServlet.class` and `DownloadServlet.class` exist in:
```
C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\jcloud\WEB-INF\classes\servlet\
```

### Problem: Download starts but file is corrupted
Ensure DataNode(s) are running — chunks cannot be retrieved if nodes are DEAD.

### Problem: files.jsp gives 404
Confirm `files.jsp` was copied into the deployment folder and into Tomcat webapps.

### Problem: Tomcat app opens but login/register fails
Redeploy — confirm updated classes are in the Tomcat `WEB-INF\classes\` folder.

---

## Day 8 Success Checklist

- Master running on `9000`
- DataNode1 and DataNode2 sending heartbeats
- `test-database.bat` completes successfully
- Tomcat serves `http://localhost:8080/jcloud`
- Register/Login works on web UI
- Dashboard shows active node count, file count, and Recent Files table
- **Download File** and **My Files** buttons on dashboard are ACTIVE (purple, not grey)
- Upload File page works at `http://localhost:8080/jcloud/upload`
- After upload — `.dat` chunk files appear in `D:\j-cloud\storage\`
- After upload — rows appear in `files`, `chunks`, `chunk_locations` tables
- My Files page at `/files.jsp` lists all files with Download buttons
- Clicking Download triggers a browser download of the reassembled file

If all are true, Day 8 is working as intended with Neon PostgreSQL.