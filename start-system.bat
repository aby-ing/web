@echo off
chcp 65001 >nul
setlocal

set "PROJECT_DIR=%~dp0"
set "BACKEND_DIR=%PROJECT_DIR%backend"
set "JAVA_HOME=D:\IntelliJ IDEA 2025.2.1\jbr"
set "MAVEN_CMD=D:\IntelliJ IDEA 2025.2.1\plugins\maven\lib\maven3\bin\mvn.cmd"
set "JAR_FILE=%BACKEND_DIR%\target\restaurant-order-system-1.0.0.jar"

set "DB_USERNAME=sa"
set "DB_PASSWORD=123456"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [错误] 找不到 Java：%JAVA_HOME%\bin\java.exe
  pause
  exit /b 1
)

set "SQL_PORT=50374"
for /f "tokens=* usebackq" %%p in (`sqlcmd -S localhost\SQLEXPRESS -U %DB_USERNAME% -P %DB_PASSWORD% -h -1 -W -Q "SET NOCOUNT ON; EXEC xp_instance_regread N'HKEY_LOCAL_MACHINE', N'Software\Microsoft\MSSQLServer\MSSQLServer\SuperSocketNetLib\Tcp\IPAll', N'TcpDynamicPorts'" ^| findstr /R "^[0-9][0-9]*$"`) do (
  set "SQL_PORT=%%p"
)
set "DB_URL=jdbc:sqlserver://localhost:%SQL_PORT%;databaseName=restaurant_order;encrypt=true;trustServerCertificate=true"

if not exist "%JAR_FILE%" (
  echo [提示] 未找到 JAR，正在自动构建项目...
  if not exist "%MAVEN_CMD%" (
    echo [错误] 找不到 Maven：%MAVEN_CMD%
    pause
    exit /b 1
  )
  cd /d "%BACKEND_DIR%"
  "%MAVEN_CMD%" -DskipTests package
  if errorlevel 1 (
    echo [错误] 构建失败，请查看上方日志。
    pause
    exit /b 1
  )
)

echo.
echo ==========================================
echo  校园餐厅点餐系统正在启动
echo  访问地址：http://localhost:8080/login.html
echo  SQL Server 端口：%SQL_PORT%
echo.
echo  普通用户：user / 123456
echo  管理员：  admin / 123456
echo.
echo  关闭服务：管理员登录后点右上角“停止服务”
echo  或者在此窗口按 Ctrl+C
echo ==========================================
echo.

start "" powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Sleep -Seconds 8; Start-Process 'http://localhost:8080/login.html'"

cd /d "%BACKEND_DIR%"
"%JAVA_HOME%\bin\java.exe" -jar "%JAR_FILE%"

echo.
echo 服务已停止。
pause
