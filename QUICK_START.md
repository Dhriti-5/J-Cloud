# 🚀 Quick Start Guide - J-Cloud

## Get J-Cloud running in 5 minutes!

---

## Prerequisites Check ✅

Before starting, verify you have:

```cmd
# Check Java
java -version
# Should show: java version "1.8.0" or higher

# Check MySQL
mysql --version
# Should show: mysql Ver 8.0.x

# Check MySQL is running
net start MySQL80
# Or check: services.msc → MySQL service
```

---

## Step 1: Setup Database (1 minute)

Your friend already created it, so just verify:

```cmd
mysql -u root -pJcloud@db
```

```sql
USE jcloud;
SHOW TABLES;
-- Should show: users, files, chunks, nodes, chunk_locations

-- Verify nodes exist (if empty, add them):
SELECT * FROM nodes;

-- If empty, run:
INSERT INTO nodes (node_name, ip_address, port, status, storage_capacity) 
VALUES 
  ('DataNode1', 'localhost', 9101, 'ACTIVE', 5000000000),
  ('DataNode2', 'localhost', 9102, 'ACTIVE', 5000000000);

EXIT;
```

---

## Step 2: Download MySQL JDBC Driver (if not done)

1. Download from: https://dev.mysql.com/downloads/connector/j/
2. Extract `mysql-connector-java-8.x.x.jar`
3. Place it in `C:\Users\Pc\J-Cloud\` directory

---

## Step 3: Compile Everything (1 minute)

```cmd
cd C:\Users\Pc\J-Cloud
compile.bat
```

Wait for "COMPILATION SUCCESSFUL!" message.

**Note:** If servlet compilation fails (servlet-api missing), that's okay. We'll compile servlets separately for Tomcat.

---

## Step 4: Test Database Connection (30 seconds)

```cmd
test-database.bat
```

Look for:
- ✓ Database connection successful
- ✓ User registered
- ✓ Authentication successful
- ✓ Node registered

---

## Step 5: Start the Cluster (2 minutes)

### Terminal 1: Master Node
```cmd
run-master.bat
```

**Wait for:**
```
╔════════════════════════════════════════════╗
║   J-CLOUD MASTER NODE STARTED              ║
║   Port: 9000                               ║
╚════════════════════════════════════════════╝
✓ Heartbeat monitor started
```

### Terminal 2: Data Node 1
Open **new terminal**:
```cmd
cd C:\Users\Pc\J-Cloud
run-datanode1.bat
```

**Wait for:**
```
✓ Registration successful!
♥ Starting heartbeat service
```

### Terminal 3: Data Node 2
Open **another terminal**:
```cmd
cd C:\Users\Pc\J-Cloud
run-datanode2.bat
```

**Wait for:**
```
✓ Registration successful!
♥ Starting heartbeat service
```

---

## Step 6: Deploy Web App (2 minutes)

**Note:** You'll need Apache Tomcat installed. If you don't have it, download from https://tomcat.apache.org/download-90.cgi

### Option A: Quick Deploy (Eclipse/IntelliJ)

1. Import project as Dynamic Web Project
2. Add MySQL JDBC driver to build path
3. Configure Tomcat server
4. Right-click → Run on Server

### Option B: Manual Deploy (Tomcat)

**IMPORTANT:** Set TOMCAT_HOME to your actual Tomcat installation path first!
```cmd
# CMD:
set TOMCAT_HOME=C:\path\to\your\apache-tomcat-9.0.xx

# PowerShell:
$env:TOMCAT_HOME = "C:\path\to\your\apache-tomcat-9.0.xx"
```

Then run the deployment commands:

```cmd
# Create deployment structure
cd C:\Users\Pc\J-Cloud
mkdir jcloud\WEB-INF\classes
mkdir jcloud\WEB-INF\lib

# Copy compiled classes
xcopy /E /I bin\shared jcloud\WEB-INF\classes\shared
xcopy /E /I bin\utils jcloud\WEB-INF\classes\utils
xcopy /E /I bin\dao jcloud\WEB-INF\classes\dao

# Compile servlets (servlet-api.jar is in project directory)
# CMD:
javac -d jcloud\WEB-INF\classes -cp "bin;servlet-api.jar;mysql-connector-java-9.5.0.jar" webapp\servlet\*.java

# PowerShell:
javac -d jcloud\WEB-INF\classes -cp "bin;servlet-api.jar;mysql-connector-java-9.5.0.jar" webapp\servlet\*.java

# Copy resources
copy webapp\*.jsp jcloud\
copy webapp\WEB-INF\web.xml jcloud\WEB-INF\
copy mysql-connector-j-9.5.0.jar jcloud\WEB-INF\lib\

# Deploy to Tomcat (update path to your actual Tomcat location)
# CMD:
xcopy /E /I jcloud %TOMCAT_HOME%\webapps\jcloud
%TOMCAT_HOME%\bin\startup.bat

# PowerShell:
Copy-Item -Recurse -Force jcloud "$env:TOMCAT_HOME\webapps\jcloud"
& "$env:TOMCAT_HOME\bin\startup.bat"
```
Copy-Item -Recurse -Force jcloud "$env:TOMCAT_HOME\webapps\jcloud"
& "$env:TOMCAT_HOME\bin\startup.bat"

---

## Step 7: Access the System! 🎉

Open your browser:

### Web Interface
```
http://localhost:8080/jcloud
```

You should see the login page!

### Test Registration
1. Click "Register here"
2. Fill in:
   - Username: `admin`
   - Email: `admin@jcloud.com`
   - Password: `REDACTED_ADMIN_USER`
   - Confirm: `REDACTED_ADMIN_USER`
3. Click "Register"

### Test Login
1. Enter username: `admin`
2. Enter password: `REDACTED_ADMIN_USER`
3. Click "Login"

### View Dashboard
You should see:
- Welcome message
- Statistics (0 files, 0 GB, 2 nodes)
- Quick action buttons

---

## ✅ System Health Check

### All Green? Check these:

**Terminal 1 (Master):**
```
♥ Heartbeat from: DataNode1
♥ Heartbeat from: DataNode2
✓ Node ALIVE: DataNode1
✓ Node ALIVE: DataNode2
```

**Terminal 2 (DataNode1):**
```
♥ Heartbeat sent and acknowledged
♥ Heartbeat sent and acknowledged
```

**Terminal 3 (DataNode2):**
```
♥ Heartbeat sent and acknowledged
♥ Heartbeat sent and acknowledged
```

**Database Check:**
```cmd
mysql -u root -pJcloud@db jcloud -e "SELECT * FROM nodes;"
```

Should show both nodes as ACTIVE.

**Web Check:**
- Can access login page ✅
- Can register user ✅
- Can login ✅
- Can see dashboard ✅

---

## 🧪 Fun Tests to Try

### Test 1: Kill a Node (Death Detection)
1. In DataNode1 terminal, press `Ctrl+C`
2. Wait 15 seconds
3. Watch Master terminal - should show: `☠ Node DEAD: DataNode1`
4. Check database: `SELECT * FROM nodes;` - DataNode1 = DEAD

### Test 2: Revive a Node
1. Restart DataNode1: `run-datanode1.bat`
2. Watch it re-register
3. Status changes back to ACTIVE

### Test 3: Multiple Users
1. Logout from dashboard
2. Register another user
3. Login with new credentials
4. Check `users` table: `SELECT * FROM users;`

---

## 🚨 Troubleshooting

### Problem: "Connection refused" when starting nodes
**Solution:** Start Master Node first, wait for "STARTED", then start Data Nodes

### Problem: "Cannot connect to database"
**Solution:** 
```cmd
net start MySQL80
mysql -u root -pJcloud@db
```

### Problem: Tomcat 404 error
**Solution:** Check deployment path is `webapps/jcloud/` not `webapps/jcloud.war/`

### Problem: Heartbeat not showing
**Solution:** Check firewall, ensure localhost connections allowed

### Problem: Login fails
**Solution:** Check if user was created: `SELECT * FROM users;`

---

## 📂 File Locations Quick Reference

```
C:\Users\Pc\J-Cloud\
├── compile.bat              ← Run first
├── test-database.bat        ← Run second
├── run-master.bat           ← Run third
├── run-datanode1.bat        ← Run fourth
├── run-datanode2.bat        ← Run fifth
└── mysql-connector-*.jar    ← Must be here!
```

---

## 🎯 What's Running?

After successful start:

| Component | Port | Status Check |
|-----------|------|--------------|
| MySQL | 3306 | `mysql -u root -pJcloud@db` |
| Master Node | 9000 | Terminal shows heartbeat monitor |
| Data Node 1 | 9101 | Terminal shows heartbeat acknowledged |
| Data Node 2 | 9102 | Terminal shows heartbeat acknowledged |
| Tomcat | 8080 | `http://localhost:8080/jcloud` |

---

## 🎉 Success!

If you see:
- ✅ Master Node running with heartbeat monitor
- ✅ Both Data Nodes sending heartbeats
- ✅ Web interface accessible
- ✅ Can login and see dashboard

**Congratulations! Your distributed file storage system is LIVE! 🚀**

---

## 📚 Next Steps

1. Read [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for detailed documentation
2. Check [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) for implementation details
3. Review [DIRECTORY_STRUCTURE.md](DIRECTORY_STRUCTURE.md) for file organization

---

**Need help? Check the console output in each terminal for error messages.**
