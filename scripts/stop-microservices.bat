@echo off
REM Stop Microservices Stack

echo ========================================
echo Stopping Neobrutalism CRM Microservices
echo ========================================
echo.

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set COMPOSE_FILE=%PROJECT_ROOT%\docker-compose.microservices.yml

echo [INFO] Stopping all services...
docker-compose -f "%COMPOSE_FILE%" down

echo.
echo [SUCCESS] All services stopped
echo.
echo To remove volumes (WARNING: deletes all data):
echo   docker-compose -f "%COMPOSE_FILE%" down -v
echo.

pause
