# Build script for all microservices (PowerShell)
# This script compiles and packages all microservices in the correct order

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Building Neobrutalism CRM Microservices" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Build counter
$successCount = 0
$totalServices = 2

# Function to build a service
function Build-Service {
    param(
        [string]$ServiceName,
        [string]$ServicePath
    )

    Write-Host "Building $ServiceName..." -ForegroundColor Yellow

    $currentLocation = Get-Location
    try {
        Set-Location $ServicePath
        mvn clean package -DskipTests

        if ($LASTEXITCODE -eq 0) {
            Write-Host "$ServiceName built successfully" -ForegroundColor Green
            $script:successCount++
            Set-Location $currentLocation
            return $true
        }
        else {
            Write-Host "$ServiceName build failed" -ForegroundColor Red
            Set-Location $currentLocation
            return $false
        }
    }
    catch {
        Write-Host "$ServiceName build failed: $_" -ForegroundColor Red
        Set-Location $currentLocation
        return $false
    }
}

# Main build process
Write-Host "Starting build process..." -ForegroundColor White
Write-Host ""

# Build Gateway Service
Build-Service "API Gateway" "microservices\gateway"
Write-Host ""

# Build IAM Service
Build-Service "IAM Service" "microservices\iam-service"
Write-Host ""

# Summary
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Build Summary" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Services built: $successCount/$totalServices" -ForegroundColor White

if ($successCount -eq $totalServices) {
    Write-Host "All microservices built successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Generated artifacts:" -ForegroundColor White
    Get-ChildItem -Path "microservices\*\target\*.jar" -Recurse | Where-Object { $_.Name -notlike "*.original" } | Select-Object FullName, @{Name="Size";Expression={"{0:N2} MB" -f ($_.Length / 1MB)}}
    exit 0
}
else {
    Write-Host "Some microservices failed to build" -ForegroundColor Red
    exit 1
}
