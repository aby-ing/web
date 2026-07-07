@echo off
chcp 65001 >nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  echo 正在停止 8080 端口进程：%%a
  taskkill /PID %%a /F
  echo 已停止。
  pause
  exit /b 0
)
echo 没有发现 8080 端口上的运行进程。
pause
