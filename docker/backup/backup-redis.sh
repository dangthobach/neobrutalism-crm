#!/bin/bash

# ========================================
# Redis Cluster Backup Script
# ========================================
# Usage: ./backup-redis.sh
# Backups RDB snapshots from all Redis master nodes

set -e

# Configuration
BACKUP_DIR="/var/backups/redis"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=7
REDIS_PASSWORD="${REDIS_PASSWORD:-redis_password_2024}"

# Redis nodes (masters only)
REDIS_NODES=(
  "crm-redis-master-1:7000"
  "crm-redis-master-2:7001"
  "crm-redis-master-3:7002"
)

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starting Redis Cluster backup..."

# Backup each master node
for NODE in "${REDIS_NODES[@]}"; do
  HOST=$(echo $NODE | cut -d: -f1)
  PORT=$(echo $NODE | cut -d: -f2)

  echo "[$(date)] Backing up $NODE..."

  # Trigger BGSAVE (background save)
  docker exec $HOST redis-cli -p $PORT -a $REDIS_PASSWORD BGSAVE

  # Wait for BGSAVE to complete (check every 1 second)
  while true; do
    LASTSAVE=$(docker exec $HOST redis-cli -p $PORT -a $REDIS_PASSWORD LASTSAVE)
    sleep 1
    NEWSAVE=$(docker exec $HOST redis-cli -p $PORT -a $REDIS_PASSWORD LASTSAVE)
    if [ "$LASTSAVE" != "$NEWSAVE" ]; then
      break
    fi
  done

  # Copy RDB file from container
  docker cp $HOST:/data/dump.rdb "$BACKUP_DIR/${HOST}_${TIMESTAMP}.rdb"

  echo "[$(date)] ✅ Backed up $NODE"
done

# Create tar archive of all RDB files
tar -czf "$BACKUP_DIR/redis-cluster_${TIMESTAMP}.tar.gz" -C "$BACKUP_DIR" *.rdb
rm "$BACKUP_DIR"/*.rdb

FILESIZE=$(du -h "$BACKUP_DIR/redis-cluster_${TIMESTAMP}.tar.gz" | cut -f1)
echo "[$(date)] ✅ Backup completed: redis-cluster_${TIMESTAMP}.tar.gz ($FILESIZE)"

# Clean up old backups
echo "[$(date)] Cleaning up backups older than $RETENTION_DAYS days..."
find "$BACKUP_DIR" -name "redis-cluster_*.tar.gz" -type f -mtime +$RETENTION_DAYS -delete

echo "[$(date)] Backup job finished"
