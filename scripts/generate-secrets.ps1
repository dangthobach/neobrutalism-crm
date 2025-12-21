# ========================================
# Security Secrets Generator (PowerShell)
# ========================================
# Generates strong random secrets for production deployment
#
# Usage:
#   .\scripts\generate-secrets.ps1

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Security Secrets Generator" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Generating strong random secrets..." -ForegroundColor Yellow
Write-Host ""

# Function to generate random base64 string
function Generate-RandomBase64 {
    param([int]$Length)
    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RNGCryptoServiceProvider]::new()
    $rng.GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}

# Function to generate alphanumeric string
function Generate-Alphanumeric {
    param([int]$Length)
    $chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    $result = ''
    $rng = [System.Security.Cryptography.RNGCryptoServiceProvider]::new()
    $bytes = New-Object byte[] $Length
    $rng.GetBytes($bytes)

    for ($i = 0; $i -lt $Length; $i++) {
        $result += $chars[$bytes[$i] % $chars.Length]
    }
    return $result
}

# Generate JWT Secret (256 bits / 32 bytes)
$JWT_SECRET = Generate-RandomBase64 -Length 32
Write-Host "✅ JWT_SECRET (256-bit):" -ForegroundColor Green
Write-Host "   $JWT_SECRET" -ForegroundColor White
Write-Host ""

# Generate Database Password (32 characters)
$DB_PASSWORD = Generate-Alphanumeric -Length 32
Write-Host "✅ DB_PASSWORD (32 characters):" -ForegroundColor Green
Write-Host "   $DB_PASSWORD" -ForegroundColor White
Write-Host ""

# Generate Redis Password (32 characters)
$REDIS_PASSWORD = Generate-Alphanumeric -Length 32
Write-Host "✅ REDIS_PASSWORD (32 characters):" -ForegroundColor Green
Write-Host "   $REDIS_PASSWORD" -ForegroundColor White
Write-Host ""

# Generate MinIO Access Key (20 characters, alphanumeric)
$MINIO_ACCESS_KEY = Generate-Alphanumeric -Length 20
Write-Host "✅ MINIO_ACCESS_KEY (20 characters):" -ForegroundColor Green
Write-Host "   $MINIO_ACCESS_KEY" -ForegroundColor White
Write-Host ""

# Generate MinIO Secret Key (40 characters)
$MINIO_SECRET_KEY = Generate-Alphanumeric -Length 40
Write-Host "✅ MINIO_SECRET_KEY (40 characters):" -ForegroundColor Green
Write-Host "   $MINIO_SECRET_KEY" -ForegroundColor White
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "IMPORTANT: Save these secrets securely!" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Recommendations:" -ForegroundColor Yellow
Write-Host "1. Store in a secure password manager"
Write-Host "2. Use environment variables in production"
Write-Host "3. Never commit these to version control"
Write-Host "4. Rotate secrets regularly (every 90 days)"
Write-Host ""

# Option to save to .env.production file
$response = Read-Host "Save to .env.production file? (y/N)"

if ($response -eq 'y' -or $response -eq 'Y') {
    # Create .env.production from .env.production.example
    if (Test-Path ".env.production.example") {
        $content = Get-Content ".env.production.example" -Raw

        # Replace placeholders with generated secrets
        $content = $content -replace 'JWT_SECRET=<CHANGE_ME_GENERATE_WITH_OPENSSL_RAND_BASE64_32>', "JWT_SECRET=$JWT_SECRET"
        $content = $content -replace 'DB_PASSWORD=<CHANGE_ME_STRONG_DB_PASSWORD_MIN_32_CHARS>', "DB_PASSWORD=$DB_PASSWORD"
        $content = $content -replace 'REDIS_PASSWORD=<CHANGE_ME_STRONG_REDIS_PASSWORD_MIN_32_CHARS>', "REDIS_PASSWORD=$REDIS_PASSWORD"
        $content = $content -replace 'MINIO_ACCESS_KEY=<CHANGE_ME_MINIO_ACCESS_KEY_MIN_20_CHARS>', "MINIO_ACCESS_KEY=$MINIO_ACCESS_KEY"
        $content = $content -replace 'MINIO_SECRET_KEY=<CHANGE_ME_MINIO_SECRET_KEY_MIN_40_CHARS>', "MINIO_SECRET_KEY=$MINIO_SECRET_KEY"

        Set-Content -Path ".env.production" -Value $content

        Write-Host ""
        Write-Host "✅ Secrets saved to .env.production" -ForegroundColor Green
        Write-Host "⚠️  Remember to update other configuration values (DB_HOST, etc.)" -ForegroundColor Yellow
    }
    else {
        Write-Host ""
        Write-Host "❌ Error: .env.production.example not found" -ForegroundColor Red
        exit 1
    }
}
else {
    Write-Host "Secrets not saved to file." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "1. Update .env.production with your database/Redis/MinIO hosts"
Write-Host "2. Update CORS_ALLOWED_ORIGINS with your production URLs"
Write-Host "3. Configure email SMTP settings"
Write-Host "4. Review SECURITY.md for full checklist"
Write-Host "5. Run security validation: mvn test"
Write-Host ""
Write-Host "Done! 🎉" -ForegroundColor Green
