@echo off
REM Start Data Node 1

echo =====================================
echo   STARTING DATA NODE 1
echo =====================================
echo.
echo Node Name: DataNode1
echo Port: 9101
echo Master: localhost:9000
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

java -cp "bin;%JDBC_DRIVER%" datanode.DataNodeServer DataNode1 9101

pause
