@echo off
REM Import Keycloak Realm Configuration (Windows)
REM This script imports the CRM realm into Keycloak

echo [INFO] Importing Keycloak realm...

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set REALM_FILE=%PROJECT_ROOT%\docker\keycloak\realm-import.json
set KEYCLOAK_URL=http://localhost:8180

REM Wait for Keycloak to be ready
echo [INFO] Waiting for Keycloak to be ready...
set MAX_ATTEMPTS=30
set ATTEMPT=1

:WAIT_LOOP
curl -sf "%KEYCLOAK_URL%/health/ready" >nul 2>&1
if %errorlevel% equ 0 (
    echo [SUCCESS] Keycloak is ready
    goto KEYCLOAK_READY
)

echo [INFO] Attempt %ATTEMPT%/%MAX_ATTEMPTS% - Keycloak not ready yet...
timeout /t 10 /nobreak >nul
set /a ATTEMPT=%ATTEMPT%+1

if %ATTEMPT% gtr %MAX_ATTEMPTS% (
    echo [ERROR] Keycloak failed to start after %MAX_ATTEMPTS% attempts
    exit /b 1
)

goto WAIT_LOOP

:KEYCLOAK_READY

REM Get admin access token
echo [INFO] Getting admin access token...

for /f "delims=" %%i in ('curl -sf -X POST "%KEYCLOAK_URL%/realms/master/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "username=admin" -d "password=admin" -d "grant_type=password" -d "client_id=admin-cli" ^| jq -r ".access_token"') do set ADMIN_TOKEN=%%i

if "%ADMIN_TOKEN%"=="" (
    echo [ERROR] Failed to get admin token
    exit /b 1
)

REM Check if realm exists
echo [INFO] Checking if realm exists...
curl -sf "%KEYCLOAK_URL%/admin/realms/crm" -H "Authorization: Bearer %ADMIN_TOKEN%" >nul 2>&1

if %errorlevel% equ 0 (
    echo [INFO] Realm 'crm' already exists
    echo [INFO] Skipping import
    exit /b 0
)

REM Import realm
echo [INFO] Importing realm from %REALM_FILE%...

curl -sf -X POST "%KEYCLOAK_URL%/admin/realms" -H "Authorization: Bearer %ADMIN_TOKEN%" -H "Content-Type: application/json" -d @"%REALM_FILE%"

if %errorlevel% equ 0 (
    echo [SUCCESS] Realm imported successfully
    echo.
    echo Realm: crm
    echo URL: %KEYCLOAK_URL%/realms/crm
    echo Admin Console: %KEYCLOAK_URL%/admin
    echo.
    echo Test Users:
    echo   - admin@crm.local / admin123 (Super Admin)
    echo   - tenant1-admin@crm.local / admin123 (Tenant Admin)
    echo   - user1@tenant1.com / user123 (User)
    exit /b 0
) else (
    echo [ERROR] Realm import failed
    exit /b 1
)
