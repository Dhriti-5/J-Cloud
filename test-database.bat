@echo off
REM Test Database Connection

echo =====================================
echo   J-CLOUD PHASE 1 DATABASE TEST
echo =====================================
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

if not exist "bin" mkdir bin

echo Recompiling required classes to avoid stale binaries...
javac -d bin shared\*.java
if %ERRORLEVEL% NEQ 0 (
	echo ERROR: Failed to compile shared classes
	pause
	exit /b 1
)

javac -d bin -cp "bin;%JDBC_DRIVER%" utils\*.java
if %ERRORLEVEL% NEQ 0 (
	echo ERROR: Failed to compile utils classes
	pause
	exit /b 1
)

javac -d bin -cp "bin;%JDBC_DRIVER%" dao\*.java
if %ERRORLEVEL% NEQ 0 (
	echo ERROR: Failed to compile DAO classes
	pause
	exit /b 1
)

javac -d bin -cp "bin;%JDBC_DRIVER%" TestPhase1.java
if %ERRORLEVEL% NEQ 0 (
	echo ERROR: Failed to compile TestPhase1.java
	echo Run compile.bat first and ensure the PostgreSQL JDBC driver exists.
	pause
	exit /b 1
)

java -cp "bin;%JDBC_DRIVER%" TestPhase1

pause
