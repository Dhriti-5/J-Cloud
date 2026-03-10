@echo off
REM Test Database Connection

echo =====================================
echo   J-CLOUD PHASE 1 DATABASE TEST
echo =====================================
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

if not exist "bin\TestPhase1.class" (
	echo TestPhase1.class not found. Compiling now...
	javac -d bin -cp "bin;%JDBC_DRIVER%" TestPhase1.java
	if %ERRORLEVEL% NEQ 0 (
		echo ERROR: Failed to compile TestPhase1.java
		echo Run compile.bat first and ensure MySQL JDBC driver exists.
		pause
		exit /b 1
	)
)

java -cp "bin;%JDBC_DRIVER%" TestPhase1

pause
