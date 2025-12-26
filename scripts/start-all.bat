@echo off
REM Start All Services Script for Windows

echo ================================================
echo   Starting Neobrutalism CRM Services
echo ================================================
echo.

REM Create logs directory if not exists
if not exist logs mkdir logs

REM Step 1: Check and start Consul
echo [1/4] Checking Consul...

netstat -an | find ":8500" >nul 2>&1
if %errorlevel% == 0 (
    echo [OK] Consul is already running on port 8500
) else (
    echo Starting Consul in dev mode...

    where consul >nul 2>&1
    if %errorlevel% neq 0 (
        echo [ERROR] Consul is not installed!
        echo Please install Consul:
        echo   Windows: choco install consul
        echo   Or download from: https://www.consul.io/downloads
        exit /b 1
    )

    start /B consul agent -dev -ui > logs\consul.log 2>&1
    echo Waiting for Consul to start...
    timeout /t 5 /nobreak >nul
    echo [OK] Consul started
    echo [OK] Consul UI: http://localhost:8500/ui
)

echo.

REM Step 2: Check Redis (optional)
echo [2/4] Checking Redis...

netstat -an | find ":6379" >nul 2>&1
if %errorlevel% == 0 (
    echo [OK] Redis is running on port 6379
) else (
    echo [WARN] Redis is not running (optional, L2 cache will be disabled)
    echo   To start Redis: docker run -d -p 6379:6379 redis
)

echo.

REM Step 3: Start Business Service
echo [3/4] Starting Business Service...

netstat -an | find ":8081" >nul 2>&1
if %errorlevel% == 0 (
    echo [WARN] Business Service is already running on port 8081
) else (
    echo Building Business Service...
    cd business-service
    call mvnw.cmd clean package -DskipTests > ..\logs\business-build.log 2>&1

    echo Starting Business Service...
    start /B cmd /c "mvnw.cmd spring-boot:run > ..\logs\business-service.log 2>&1"

    cd ..

    echo Waiting for Business Service to start...
    timeout /t 15 /nobreak >nul

    curl -f -s http://localhost:8081/actuator/health >nul 2>&1
    if %errorlevel% == 0 (
        echo [OK] Business Service is healthy
        echo [OK] Business Service API: http://localhost:8081/swagger-ui.html
    ) else (
        echo [ERROR] Business Service failed to start
        echo Check logs: type logs\business-service.log
        exit /b 1
    )
)

echo.

REM Step 4: Start Gateway
echo [4/4] Starting Gateway Service...

netstat -an | find ":8080" >nul 2>&1
if %errorlevel% == 0 (
    echo [WARN] Gateway is already running on port 8080
) else (
    echo Building Gateway...
    cd gateway-service
    call mvnw.cmd clean package -DskipTests > ..\logs\gateway-build.log 2>&1

    echo Starting Gateway...
    start /B cmd /c "mvnw.cmd spring-boot:run > ..\logs\gateway-service.log 2>&1"

    cd ..

    echo Waiting for Gateway to start...
    timeout /t 15 /nobreak >nul

    curl -f -s http://localhost:8080/actuator/health >nul 2>&1
    if %errorlevel% == 0 (
        echo [OK] Gateway is healthy
        echo [OK] Gateway API: http://localhost:8080
    ) else (
        echo [ERROR] Gateway failed to start
        echo Check logs: type logs\gateway-service.log
        exit /b 1
    )
)

echo.
echo ================================================
echo   All Services Started Successfully!
echo ================================================
echo.
echo Service URLs:
echo   Consul UI:        http://localhost:8500/ui
echo   Gateway:          http://localhost:8080
echo   Business Service: http://localhost:8081
echo   Swagger UI:       http://localhost:8081/swagger-ui.html
echo.
echo Health Checks:
echo   Gateway:  curl http://localhost:8080/actuator/health
echo   Business: curl http://localhost:8081/actuator/health
echo.
echo Service Discovery:
echo   curl http://localhost:8500/v1/catalog/services
echo.
echo Logs:
echo   Consul:   type logs\consul.log
echo   Gateway:  type logs\gateway-service.log
echo   Business: type logs\business-service.log
echo.
echo To stop all services:
echo   scripts\stop-all.bat
echo.

pause
