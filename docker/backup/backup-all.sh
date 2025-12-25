#!/bin/bash

# ========================================
# All Services Backup Script
# ========================================
# Master script to backup all services

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "Starting Full System Backup"
echo "Timestamp: $(date)"
echo "========================================="

# 1. Backup PostgreSQL
echo ""
echo ">>> PostgreSQL Backup"
bash "$SCRIPT_DIR/backup-postgres.sh"

# 2. Backup Redis
echo ""
echo ">>> Redis Cluster Backup"
bash "$SCRIPT_DIR/backup-redis.sh"

# 3. Backup Keycloak
echo ""
echo ">>> Keycloak Backup"
bash "$SCRIPT_DIR/backup-keycloak.sh"

echo ""
echo "========================================="
echo "✅ Full System Backup Completed"
echo "Timestamp: $(date)"
echo "========================================="
