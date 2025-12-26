@echo off
REM Stop All Services Script for Windows

echo ================================================
echo   Stopping All Services
echo ================================================
echo.

REM Function to stop service by port
set PORT=8080
set SERVICE=Gateway
call :StopByPort

set PORT=8081
set SERVICE=Business Service
call :StopByPort

set PORT=8500
set SERVICE=Consul
call :StopByPort

echo.
echo ================================================
echo   All Services Stopped
echo ================================================
echo.

goto :EOF

:StopByPort
    echo Stopping %SERVICE% (port %PORT%)...

    for /f "tokens=5" %%a in ('netstat -aon ^| find ":%PORT%" ^| find "LISTENING"') do (
        set PID=%%a
    )

    if defined PID (
        taskkill /F /PID %PID% >nul 2>&1
        echo [OK] %SERVICE% stopped
    ) else (
        echo [WARN] %SERVICE% was not running
    )

    set PID=
    goto :EOF
