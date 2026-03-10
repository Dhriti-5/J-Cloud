@echo off
setlocal
REM Compile all Java files for J-Cloud project
REM Make sure MySQL JDBC driver is in the current directory

echo =====================================
echo   J-CLOUD PROJECT COMPILATION
echo =====================================
echo.

REM Set classpath with MySQL JDBC driver
set JDBC_DRIVER=mysql-connector-java-8.0.33.jar
set CLASSPATH=.;%JDBC_DRIVER%

REM Create bin directory if it doesn't exist
if not exist "bin" mkdir bin

echo [1/6] Compiling shared classes...
javac -d bin shared\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile shared classes
    pause
    exit /b 1
)

echo [2/6] Compiling utils classes...
javac -d bin -cp bin utils\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to compile utils classes
    pause
    exit /b 1
)

echo [3/6] Compiling DAO classes...
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

REM Try CATALINA_HOME first
if defined CATALINA_HOME (
    if exist "%CATALINA_HOME%\lib\servlet-api.jar" (
        set SERVLET_API_JAR=%CATALINA_HOME%\lib\servlet-api.jar
    )
)

REM Try common Tomcat install paths if CATALINA_HOME is not set
if not defined SERVLET_API_JAR (
    for %%P in (
        "C:\apache-tomcat-10.1.30\lib\servlet-api.jar"
        "C:\apache-tomcat-10.1.28\lib\servlet-api.jar"
        "C:\apache-tomcat-9.0.89\lib\servlet-api.jar"
        "C:\apache-tomcat-9.0.87\lib\servlet-api.jar"
        "C:\Program Files\Apache Software Foundation\Tomcat 10.1\lib\servlet-api.jar"
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
