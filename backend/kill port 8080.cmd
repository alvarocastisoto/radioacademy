@echo off
:: --- Auto-elevación ---
net session >nul 2>&1
if %errorlevel% neq 0 (
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)

set PORT=8080
for /f "tokens=5" %%p in ('netstat -aon ^| findstr /R /C:":%PORT% .*LISTENING"') do (
  echo Matando PID %%p en puerto %PORT%
  taskkill /F /PID %%p >nul 2>&1
)
echo Hecho.
