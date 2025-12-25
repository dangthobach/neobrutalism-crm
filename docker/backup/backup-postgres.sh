#!/bin/bash

# ========================================
# PostgreSQL Backup Script
# ========================================
# Usage: ./backup-postgres.sh
# Requires: PostgreSQL client tools (pg_dump)

set -e

# Configuration
BACKUP_DIR="/var/backups/postgres"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=7

# Database connection
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-neobrutalism_crm}"
DB_USER="${DB_USER:-crm_user}"
PGPASSWORD="${DB_PASSWORD:-crm_password_2024}"

# Backup filename
BACKUP_FILE="$BACKUP_DIR/postgres_${DB_NAME}_${TIMESTAMP}.dump"

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starting PostgreSQL backup..."

# Create backup using custom format (compressed)
PGPASSWORD="$PGPASSWORD" pg_dump \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -Fc \
  -f "$BACKUP_FILE" \
  "$DB_NAME"

# Verify backup file exists
if [ -f "$BACKUP_FILE" ]; then
  FILESIZE=$(du -h "$BACKUP_FILE" | cut -f1)
  echo "[$(date)] ✅ Backup completed: $BACKUP_FILE ($FILESIZE)"
else
  echo "[$(date)] ❌ Backup failed!"
  exit 1
fi

# Clean up old backups (keep last 7 days)
echo "[$(date)] Cleaning up backups older than $RETENTION_DAYS days..."
find "$BACKUP_DIR" -name "postgres_*.dump" -type f -mtime +$RETENTION_DAYS -delete
echo "[$(date)] Cleanup completed"

# Optional: Upload to S3 (uncomment if using AWS)
# if command -v aws &> /dev/null; then
#   aws s3 cp "$BACKUP_FILE" "s3://your-bucket/postgres-backups/"
#   echo "[$(date)] Uploaded to S3"
# fi

echo "[$(date)] Backup job finished"
