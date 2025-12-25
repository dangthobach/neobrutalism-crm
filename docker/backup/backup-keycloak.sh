#!/bin/bash

# ========================================
# Keycloak Backup Script
# ========================================
# Usage: ./backup-keycloak.sh
# Backs up Keycloak realms and database

set -e

# Configuration
BACKUP_DIR="/var/backups/keycloak"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=7

# Keycloak settings
KEYCLOAK_CONTAINER="crm-keycloak"
KEYCLOAK_REALM="neobrutalism-crm"

# Database connection
DB_HOST="${KEYCLOAK_DB_HOST:-localhost}"
DB_PORT="${KEYCLOAK_DB_PORT:-5432}"
DB_NAME="keycloak"
DB_USER="${KEYCLOAK_DB_USER:-keycloak_user}"
PGPASSWORD="${KEYCLOAK_DB_PASSWORD:-keycloak_password_2024}"

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starting Keycloak backup..."

# ========================================
# 1. Export Realm Configuration
# ========================================
echo "[$(date)] Exporting realm: $KEYCLOAK_REALM..."

docker exec $KEYCLOAK_CONTAINER \
  /opt/keycloak/bin/kc.sh export \
  --dir /tmp/keycloak-export \
  --realm $KEYCLOAK_REALM

# Copy exported realm
docker cp $KEYCLOAK_CONTAINER:/tmp/keycloak-export/$KEYCLOAK_REALM-realm.json \
  "$BACKUP_DIR/${KEYCLOAK_REALM}_${TIMESTAMP}.json"

echo "[$(date)] ✅ Realm exported"

# ========================================
# 2. Backup Keycloak Database
# ========================================
echo "[$(date)] Backing up Keycloak database..."

PGPASSWORD="$PGPASSWORD" pg_dump \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -Fc \
  -f "$BACKUP_DIR/keycloak-db_${TIMESTAMP}.dump" \
  "$DB_NAME"

echo "[$(date)] ✅ Database backed up"

# ========================================
# 3. Create Combined Archive
# ========================================
tar -czf "$BACKUP_DIR/keycloak-full_${TIMESTAMP}.tar.gz" \
  -C "$BACKUP_DIR" \
  "${KEYCLOAK_REALM}_${TIMESTAMP}.json" \
  "keycloak-db_${TIMESTAMP}.dump"

# Remove individual files
rm "$BACKUP_DIR/${KEYCLOAK_REALM}_${TIMESTAMP}.json"
rm "$BACKUP_DIR/keycloak-db_${TIMESTAMP}.dump"

FILESIZE=$(du -h "$BACKUP_DIR/keycloak-full_${TIMESTAMP}.tar.gz" | cut -f1)
echo "[$(date)] ✅ Full backup completed: keycloak-full_${TIMESTAMP}.tar.gz ($FILESIZE)"

# ========================================
# 4. Clean Up Old Backups
# ========================================
echo "[$(date)] Cleaning up backups older than $RETENTION_DAYS days..."
find "$BACKUP_DIR" -name "keycloak-full_*.tar.gz" -type f -mtime +$RETENTION_DAYS -delete

echo "[$(date)] Backup job finished"
