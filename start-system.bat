@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion

set "PROJECT_DIR=%~dp0"
set "BACKEND_DIR=%PROJECT_DIR%backend"
set "JAR_FILE=%BACKEND_DIR%\target\restaurant-order-system-1.0.0.jar"

if not defined DB_USERNAME set "DB_USERNAME=root"
if not defined DB_PASSWORD set "DB_PASSWORD=123456"

if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

if not defined JAVA_EXE (
  for /f "tokens=* usebackq" %%j in (`where java 2^>nul`) do (
    if not defined JAVA_EXE set "JAVA_EXE=%%j"
  )
)

if not defined JAVA_EXE (
  echo [ERROR] Java was not found. Install JDK 21+ or set JAVA_HOME.
  pause
  exit /b 1
)

if not defined MAVEN_CMD (
  if defined MAVEN_HOME (
    if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
  )
)

if not defined MAVEN_CMD (
  for /f "tokens=* usebackq" %%m in (`dir /b /s "D:\Maven\mvn.cmd" "D:\Maven\apache-maven-*\bin\mvn.cmd" 2^>nul`) do (
    if not defined MAVEN_CMD set "MAVEN_CMD=%%m"
  )
)

if not defined MAVEN_CMD (
  for /f "tokens=* usebackq" %%m in (`where mvn.cmd 2^>nul`) do (
    if not defined MAVEN_CMD set "MAVEN_CMD=%%m"
  )
)

if not defined MAVEN_CMD (
  for /f "tokens=* usebackq" %%m in (`where mvn 2^>nul`) do (
    if not defined MAVEN_CMD set "MAVEN_CMD=%%m"
  )
)

if not defined DB_URL (
  set "DB_URL=jdbc:mysql://localhost:3306/restaurant_order?useUnicode=true^&characterEncoding=utf8^&serverTimezone=Asia/Shanghai^&useSSL=false^&allowPublicKeyRetrieval=true"
)

if not exist "%JAR_FILE%" (
  echo [INFO] JAR not found. Building the backend first...
  if not defined MAVEN_CMD (
    echo [ERROR] Maven was not found. Install Maven or set MAVEN_HOME/MAVEN_CMD.
    pause
    exit /b 1
  )
  cd /d "%BACKEND_DIR%"
  "%MAVEN_CMD%" -DskipTests package
  if errorlevel 1 (
    echo [ERROR] Build failed. Check the logs above.
    pause
    exit /b 1
  )
)

echo.
echo ==========================================
echo  Campus restaurant order system starting
echo  URL: http://localhost:8080/login.html
echo  Java: %JAVA_EXE%
echo  MySQL: %DB_URL%
echo.
echo  User:  user  / 123456
echo  Admin: admin / 123456
echo.
echo  Press Ctrl+C in this window to stop.
echo ==========================================
echo.

start "" powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Sleep -Seconds 8; Start-Process 'http://localhost:8080/login.html'"

cd /d "%BACKEND_DIR%"
"%JAVA_EXE%" -jar "%JAR_FILE%"

echo.
echo Service stopped.
pause
