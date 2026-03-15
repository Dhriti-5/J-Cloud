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
for %%F in (postgresql-*.jar) do (
	if not defined JDBC_DRIVER set JDBC_DRIVER=%%F
)

if not defined JDBC_DRIVER (
	for %%F in (webapp\WEB-INF\lib\postgresql-*.jar jcloud\WEB-INF\lib\postgresql-*.jar) do (
		if not defined JDBC_DRIVER set JDBC_DRIVER=%%F
	)
)

if not defined JDBC_DRIVER (
	echo ERROR: PostgreSQL JDBC driver jar not found.
	echo Place postgresql-*.jar in the project root, webapp\WEB-INF\lib, or jcloud\WEB-INF\lib.
	pause
	exit /b 1
)

echo Using JDBC driver: %JDBC_DRIVER%

java -cp "bin;%JDBC_DRIVER%" master.MasterServer

pause
