@echo off
REM Start Master Node Server

echo =====================================
echo   STARTING J-CLOUD MASTER NODE
echo =====================================
echo.
echo Port: 9000
echo Thread Pool: 50 threads
echo Heartbeat Monitor: Enabled
echo.

set JDBC_DRIVER=
for %%F in (mysql-connector-j-*.jar mysql-connector-java-*.jar) do (
	if not defined JDBC_DRIVER set JDBC_DRIVER=%%F
)

if not defined JDBC_DRIVER (
	echo ERROR: MySQL JDBC driver jar not found in project root.
	echo Place one of these files in this folder:
	echo   - mysql-connector-j-*.jar
	echo   - mysql-connector-java-*.jar
	pause
	exit /b 1
)

echo Using JDBC driver: %JDBC_DRIVER%

java -cp "bin;%JDBC_DRIVER%" master.MasterServer

pause
