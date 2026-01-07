@echo off
REM Consul Service Mesh Shutdown Script for Windows

echo =========================================
echo Stopping Consul Service Mesh
echo =========================================
echo.

cd ..\..

echo Stopping all services...
docker-compose -f docker-compose.service-mesh.yml down

if %errorlevel% neq 0 (
    echo ERROR: Failed to stop services
    pause
    exit /b 1
)

echo.
echo Services stopped successfully!
echo.
echo To remove volumes (WARNING: deletes all data):
echo   docker-compose -f docker-compose.service-mesh.yml down -v
echo.

pause
