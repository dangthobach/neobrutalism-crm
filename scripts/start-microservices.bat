@echo off
REM Start Microservices Stack
REM Windows startup script for local development

echo ========================================
echo Starting Neobrutalism CRM Microservices
echo ========================================
echo.

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set COMPOSE_FILE=%PROJECT_ROOT%\docker-compose.microservices.yml

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running. Please start Docker Desktop.
    exit /b 1
)

echo [INFO] Starting infrastructure services...
docker-compose -f "%COMPOSE_FILE%" up -d postgres redis-node-1 redis-node-2 redis-node-3 keycloak minio prometheus grafana zipkin

echo.
echo [INFO] Waiting for infrastructure to be healthy...
timeout /t 30 /nobreak >nul

echo.
echo [INFO] Importing Keycloak realm...
call "%SCRIPT_DIR%import-keycloak-realm.bat"

echo.
echo [INFO] Importing Casbin policies...
docker-compose -f "%COMPOSE_FILE%" exec -T postgres psql -U crm_user -d iam_db < "%SCRIPT_DIR%init-casbin-policies.sql"

echo.
echo [INFO] Starting microservices...
docker-compose -f "%COMPOSE_FILE%" up -d gateway iam-service

echo.
echo [INFO] Waiting for services to be healthy...
timeout /t 20 /nobreak >nul

echo.
echo ========================================
echo Microservices started successfully!
echo ========================================
echo.
echo Services:
echo   Gateway:        http://localhost:8080
echo   IAM Service:    http://localhost:8081
echo   Keycloak:       http://localhost:8180 (admin/admin)
echo   Grafana:        http://localhost:3001 (admin/admin)
echo   Prometheus:     http://localhost:9090
echo   Zipkin:         http://localhost:9411
echo.
echo Check status:
echo   docker-compose -f "%COMPOSE_FILE%" ps
echo.
echo View logs:
echo   docker-compose -f "%COMPOSE_FILE%" logs -f gateway
echo   docker-compose -f "%COMPOSE_FILE%" logs -f iam-service
echo.

pause
