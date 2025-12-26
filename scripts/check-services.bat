@echo off
REM Check Services Health Script for Windows

echo ================================================
echo   Service Health Check
echo ================================================
echo.

REM Check Consul
echo Checking Consul...
curl -f -s http://localhost:8500/v1/status/leader >nul 2>&1
if %errorlevel% == 0 (
    echo [OK] Consul - Healthy
) else (
    echo [ERROR] Consul - Unhealthy
)

REM Check Business Service
echo Checking Business Service...
curl -f -s http://localhost:8081/actuator/health >nul 2>&1
if %errorlevel% == 0 (
    echo [OK] Business Service - Healthy
) else (
    echo [ERROR] Business Service - Unhealthy
)

REM Check Gateway
echo Checking Gateway...
curl -f -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% == 0 (
    echo [OK] Gateway - Healthy
) else (
    echo [ERROR] Gateway - Unhealthy
)

echo.
echo Service Discovery:
echo.

REM Check registered services
curl -s http://localhost:8500/v1/catalog/services 2>nul

echo.
echo.
echo Gateway Routes:
echo.

REM Check gateway routes
curl -s http://localhost:8080/actuator/gateway/routes 2>nul

echo.
pause
