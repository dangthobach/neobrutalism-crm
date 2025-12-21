#!/bin/bash

# ========================================
# Security Secrets Generator
# ========================================
# Generates strong random secrets for production deployment
#
# Usage:
#   chmod +x scripts/generate-secrets.sh
#   ./scripts/generate-secrets.sh

set -e

echo "=========================================="
echo "Security Secrets Generator"
echo "=========================================="
echo ""

# Check if openssl is available
if ! command -v openssl &> /dev/null; then
    echo "Error: openssl is not installed. Please install it first."
    exit 1
fi

echo "Generating strong random secrets..."
echo ""

# Generate JWT Secret (256 bits / 32 bytes)
JWT_SECRET=$(openssl rand -base64 32)
echo "✅ JWT_SECRET (256-bit):"
echo "   $JWT_SECRET"
echo ""

# Generate Database Password (32 characters)
DB_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-32)
echo "✅ DB_PASSWORD (32 characters):"
echo "   $DB_PASSWORD"
echo ""

# Generate Redis Password (32 characters)
REDIS_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-32)
echo "✅ REDIS_PASSWORD (32 characters):"
echo "   $REDIS_PASSWORD"
echo ""

# Generate MinIO Access Key (20 characters, alphanumeric)
MINIO_ACCESS_KEY=$(openssl rand -base64 15 | tr -d "=+/" | cut -c1-20)
echo "✅ MINIO_ACCESS_KEY (20 characters):"
echo "   $MINIO_ACCESS_KEY"
echo ""

# Generate MinIO Secret Key (40 characters)
MINIO_SECRET_KEY=$(openssl rand -base64 30 | tr -d "=+/" | cut -c1-40)
echo "✅ MINIO_SECRET_KEY (40 characters):"
echo "   $MINIO_SECRET_KEY"
echo ""

echo "=========================================="
echo "IMPORTANT: Save these secrets securely!"
echo "=========================================="
echo ""
echo "Recommendations:"
echo "1. Store in a secure password manager"
echo "2. Use environment variables in production"
echo "3. Never commit these to version control"
echo "4. Rotate secrets regularly (every 90 days)"
echo ""

# Option to save to .env.production file
read -p "Save to .env.production file? (y/N): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Create .env.production from .env.production.example
    if [ -f ".env.production.example" ]; then
        cp .env.production.example .env.production

        # Replace placeholders with generated secrets
        sed -i.bak "s|JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|" .env.production
        sed -i.bak "s|DB_PASSWORD=.*|DB_PASSWORD=$DB_PASSWORD|" .env.production
        sed -i.bak "s|REDIS_PASSWORD=.*|REDIS_PASSWORD=$REDIS_PASSWORD|" .env.production
        sed -i.bak "s|MINIO_ACCESS_KEY=.*|MINIO_ACCESS_KEY=$MINIO_ACCESS_KEY|" .env.production
        sed -i.bak "s|MINIO_SECRET_KEY=.*|MINIO_SECRET_KEY=$MINIO_SECRET_KEY|" .env.production

        rm .env.production.bak

        echo "✅ Secrets saved to .env.production"
        echo "⚠️  Remember to update other configuration values (DB_HOST, etc.)"
    else
        echo "❌ Error: .env.production.example not found"
        exit 1
    fi
else
    echo "Secrets not saved to file."
fi

echo ""
echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Update .env.production with your database/Redis/MinIO hosts"
echo "2. Update CORS_ALLOWED_ORIGINS with your production URLs"
echo "3. Configure email SMTP settings"
echo "4. Review SECURITY.md for full checklist"
echo "5. Run security validation: mvn test"
echo ""
echo "Done! 🎉"
