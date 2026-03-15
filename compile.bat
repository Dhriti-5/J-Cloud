@echo off
setlocal
REM Compile all Java files for J-Cloud project
REM Make sure PostgreSQL JDBC driver is available

echo =====================================
echo   J-CLOUD PROJECT COMPILATION
echo =====================================
echo.

set JDBC_DRIVER=
for %%F in (postgresql-*.jar) do (
    if not defined JDBC_DRIVER set JDBC_DRIVER=%%F
)

if not defined JDBC_DRIVER (
    if exist "webapp\WEB-INF\lib" (
        for %%F in (webapp\WEB-INF\lib\postgresql-*.jar) do (
            if not defined JDBC_DRIVER set JDBC_DRIVER=%%F
        )
    )
)

if not defined JDBC_DRIVER (
    if exist "jcloud\WEB-INF\lib" (
        for %%F in (jcloud\WEB-INF\lib\postgresql-*.jar) do (
            if not defined JDBC_DRIVER set JDBC_DRIVER=%%F
        )
    )
)

if not defined JDBC_DRIVER (
    echo ERROR: PostgreSQL JDBC driver jar not found.
    echo Place postgresql-*.jar in the project root, webapp\WEB-INF\lib, or jcloud\WEB-INF\lib.
    pause
    exit /b 1
)

set CLASSPATH=.;%JDBC_DRIVER%
echo Using JDBC driver: %JDBC_DRIVER%

REM Create bin directory if it doesn't exist
if not exist "bin" mkdir bin

echo [1/7] Compiling shared classes...
javac -d bin shared\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile shared classes
    pause
    exit /b 1
)

echo [2/7] Compiling utils classes...
javac -d bin -cp bin utils\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile utils classes
    pause
    exit /b 1
)

echo [3/7] Compiling DAO classes...
javac -d bin -cp "bin;%JDBC_DRIVER%" dao\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile DAO classes
    pause
    exit /b 1
)

echo [4/7] Compiling master node classes...
javac -d bin -cp "bin;%JDBC_DRIVER%" master\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile master classes
    pause
    exit /b 1
)

echo [5/7] Compiling data node classes...
javac -d bin -cp "bin;%JDBC_DRIVER%" datanode\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile datanode classes
    pause
    exit /b 1
)

echo [6/7] Compiling test classes...
javac -d bin -cp "bin;%JDBC_DRIVER%" TestPhase1.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile TestPhase1.java
    pause
    exit /b 1
)

echo [7/7] Compiling servlet classes...
set SERVLET_API_JAR=

REM Prefer local servlet-api.jar in project root if present
if exist "servlet-api.jar" (
    set SERVLET_API_JAR=servlet-api.jar
)

REM Try CATALINA_HOME first
if not defined SERVLET_API_JAR if defined CATALINA_HOME (
    if exist "%CATALINA_HOME%\lib\servlet-api.jar" (
        set SERVLET_API_JAR=%CATALINA_HOME%\lib\servlet-api.jar
    )
)

REM Try common Tomcat install paths if CATALINA_HOME is not set
if not defined SERVLET_API_JAR (
    for %%P in (
        "C:\apache-tomcat-9.0.89\lib\servlet-api.jar"
        "C:\apache-tomcat-9.0.87\lib\servlet-api.jar"
        "C:\Program Files\Apache Software Foundation\Tomcat 9.0\lib\servlet-api.jar"
    ) do (
        if not defined SERVLET_API_JAR if exist %%~P set SERVLET_API_JAR=%%~P
    )
)

if defined SERVLET_API_JAR (
    echo Found servlet-api.jar: %SERVLET_API_JAR%
    javac -d bin -cp "bin;%JDBC_DRIVER%;%SERVLET_API_JAR%" webapp\servlet\*.java
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Failed to compile servlet classes
        pause
        exit /b 1
    )
) else (
    echo WARNING: servlet-api.jar not found. Skipping servlet compilation.
    echo To compile servlets, set CATALINA_HOME to your Tomcat folder.
)

echo.
echo =====================================
echo   COMPILATION SUCCESSFUL!
echo =====================================
echo.
echo Compiled classes are in: bin\
echo.
pause
