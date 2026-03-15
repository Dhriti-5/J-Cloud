# 🚀 Quick Start Guide - J-Cloud (Neon PostgreSQL)

## Get J-Cloud running with Neon + Tomcat

---

## Prerequisites Check ✅

Before starting, verify:

```cmd
java -version
```

Expected: Java 8+.

Also verify these files exist:

- `jcloud\WEB-INF\lib\postgresql-42.7.10.jar`
- `.env`

---

## Step 1: Configure Database URL in `.env`

Project root file `.env` must contain your Neon URL:

```text
JCLOUD_DB_URL=postgresql://<user>:<password>@<host>/<database>?sslmode=require&channel_binding=require
```

Do not use localhost DB settings.

---

## Step 2: Compile Project

```cmd
cd C:\Users\Pc\J-Cloud
compile.bat
```

Wait for compilation success.

---

## Step 3: Verify DB + Phase 1 quickly

```cmd
test-database.bat
```

Expected outcomes:

- Database connection successful
- User registration/authentication successful
- Node registration + fetch successful

This confirms the app is using Neon PostgreSQL correctly.

---

## Step 4: Start Day 3 Services (Master + Data Nodes)

### Terminal 1

```cmd
run-master.bat
```

### Terminal 2

```cmd
cd C:\Users\Pc\J-Cloud
run-datanode1.bat
```

### Terminal 3

```cmd
cd C:\Users\Pc\J-Cloud
run-datanode2.bat
```

Expected Day 3 behavior:

- both data nodes register with master
- periodic heartbeats are logged
- nodes are marked ACTIVE in DB

---

## Step 5: Redeploy Day 4 Web App to Tomcat

Yes, redeploy is recommended after DB layer changes.

Important: do a clean redeploy so Tomcat cannot reuse old classes from previous deployments.

Set Tomcat path first:

```cmd
set TOMCAT_HOME=C:\path\to\apache-tomcat-9.0.xx
```

```powershell
$env:TOMCAT_HOME = "C:\path\to\apache-tomcat-9.0.xx"
$env:CATALINA_HOME = $env:TOMCAT_HOME

# Make Neon DB URL available to Tomcat process
$env:JCLOUD_DB_URL = "postgresql://<user>:<password>@<host>/<database>?sslmode=require&channel_binding=require"
```

Then run:

```cmd
cd C:\Users\Pc\J-Cloud

if not exist jcloud\WEB-INF\classes mkdir jcloud\WEB-INF\classes
if not exist jcloud\WEB-INF\lib mkdir jcloud\WEB-INF\lib

xcopy /E /I /Y bin\shared jcloud\WEB-INF\classes\shared
xcopy /E /I /Y bin\utils jcloud\WEB-INF\classes\utils
xcopy /E /I /Y bin\dao jcloud\WEB-INF\classes\dao

javac -d jcloud\WEB-INF\classes -cp "bin;servlet-api.jar;jcloud\WEB-INF\lib\postgresql-42.7.10.jar" webapp\servlet\*.java

copy /Y webapp\*.jsp jcloud\
copy /Y webapp\WEB-INF\web.xml jcloud\WEB-INF\
copy /Y jcloud\WEB-INF\lib\postgresql-42.7.10.jar jcloud\WEB-INF\lib\

xcopy /E /I /Y jcloud %TOMCAT_HOME%\webapps\jcloud
```

PowerShell equivalent:

```powershell
Set-Location C:\Users\Pc\J-Cloud

# Clean old deployment and Tomcat cache first (prevents stale classes)
if (Test-Path "$env:TOMCAT_HOME\webapps\jcloud") { Remove-Item -Recurse -Force "$env:TOMCAT_HOME\webapps\jcloud" }
if (Test-Path "$env:TOMCAT_HOME\work\Catalina\localhost\jcloud") { Remove-Item -Recurse -Force "$env:TOMCAT_HOME\work\Catalina\localhost\jcloud" }

if (-not (Test-Path .\jcloud\WEB-INF\classes)) { New-Item -ItemType Directory -Path .\jcloud\WEB-INF\classes | Out-Null }
if (-not (Test-Path .\jcloud\WEB-INF\lib)) { New-Item -ItemType Directory -Path .\jcloud\WEB-INF\lib | Out-Null }

xcopy /E /I /Y bin\shared jcloud\WEB-INF\classes\shared
xcopy /E /I /Y bin\utils jcloud\WEB-INF\classes\utils
xcopy /E /I /Y bin\dao jcloud\WEB-INF\classes\dao

$src = Get-ChildItem .\webapp\servlet\*.java | ForEach-Object FullName
javac -d jcloud\WEB-INF\classes -cp "bin;servlet-api.jar;jcloud\WEB-INF\lib\postgresql-42.7.10.jar" $src

Copy-Item .\webapp\*.jsp .\jcloud\ -Force
Copy-Item .\webapp\WEB-INF\web.xml .\jcloud\WEB-INF\ -Force

Copy-Item -Recurse -Force .\jcloud "$env:TOMCAT_HOME\webapps\jcloud"
```

Restart Tomcat:

```cmd
%TOMCAT_HOME%\bin\shutdown.bat
%TOMCAT_HOME%\bin\startup.bat
```

```powershell
& "$env:TOMCAT_HOME\bin\shutdown.bat"
& "$env:TOMCAT_HOME\bin\startup.bat"
```

If startup still shows old MySQL stack traces, stop Tomcat again and delete these folders manually, then start Tomcat:

- $env:TOMCAT_HOME\webapps\jcloud
- $env:TOMCAT_HOME\work\Catalina\localhost\jcloud

---

## Step 6: Validate Day 4 Web Flow

Open:

```text
http://localhost:8080/jcloud
```

Validate:

- register user
- login user
- dashboard opens
- nodes shown as active

---

## Troubleshooting

### Problem: DB connection still goes to localhost

Cause: `.env` missing or wrong `JCLOUD_DB_URL`.

Fix: update `.env` and rerun `test-database.bat`.

### Problem: PostgreSQL driver not found

Ensure this jar exists:

- `jcloud\WEB-INF\lib\postgresql-42.7.10.jar`

### Problem: Tomcat app opens but login/register fails

Redeploy again and confirm updated classes are copied from `bin\utils` and `bin\dao`.

---

## Day 3 + Day 4 Success Checklist

- Master running on `9000`
- DataNode1 and DataNode2 sending heartbeats
- `test-database.bat` completes successfully
- Tomcat serves `http://localhost:8080/jcloud`
- Register/Login works on web UI

If all are true, Day 3 and Day 4 are working as intended with Neon PostgreSQL.
