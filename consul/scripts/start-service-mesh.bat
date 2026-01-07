@echo off
REM Consul Service Mesh Startup Script for Windows
REM This script starts the entire service mesh stack

echo =========================================
echo Consul Service Mesh Startup
echo =========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo [1/5] Building Java services...
echo.

REM Build Gateway Service
echo   Building gateway-service...
cd ..\..\..\gateway-service
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: Failed to build gateway-service
    pause
    exit /b 1
)

REM Build Business Service
echo   Building business-service...
cd ..\business-service
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: Failed to build business-service
    pause
    exit /b 1
)

cd ..

echo.
echo [2/5] Starting Docker Compose services...
echo.

REM Start all services
docker-compose -f docker-compose.service-mesh.yml up -d

if %errorlevel% neq 0 (
    echo ERROR: Failed to start Docker services
    pause
    exit /b 1
)

echo.
echo [3/5] Waiting for Consul to be ready...
echo.

:wait_consul
timeout /t 2 /nobreak >nul
curl -sf http://localhost:8500/v1/status/leader >nul 2>&1
if %errorlevel% neq 0 (
    echo   Consul not ready yet, waiting...
    goto wait_consul
)

echo   Consul is ready!

echo.
echo [4/5] Waiting for services to start...
echo.

REM Wait 60 seconds for services to fully start
timeout /t 60 /nobreak

echo.
echo [5/5] Registering services with Consul...
echo.

REM Note: This requires bash/WSL for the shell script
REM For pure Windows, you would need to convert register-services.sh to PowerShell

wsl bash -c "cd consul/scripts && ./register-services.sh"

if %errorlevel% neq 0 (
    echo WARNING: Service registration script requires WSL
    echo Please run manually: cd consul\scripts ^&^& bash register-services.sh
)

echo.
echo =========================================
echo Service Mesh Started Successfully!
echo =========================================
echo.
echo Access the following UIs:
echo.
echo   Consul UI:     http://localhost:8500
echo   Grafana:       http://localhost:3000  (admin/admin123)
echo   Jaeger:        http://localhost:16686
echo   Prometheus:    http://localhost:9090
echo   Gateway API:   http://localhost:8080
echo.
echo To check service health, run:
echo   docker-compose -f docker-compose.service-mesh.yml ps
echo.
echo To view logs:
echo   docker-compose -f docker-compose.service-mesh.yml logs -f
echo.

pause
