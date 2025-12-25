#!/bin/bash

# Script to update Redis nodes with persistence settings
# Run this if you need to manually update docker-compose.microservices.yml

PERSISTENCE_CONFIG="
      --appendonly yes
      --appendfsync everysec
      --auto-aof-rewrite-percentage 100
      --auto-aof-rewrite-min-size 64mb
      --save 900 1
      --save 300 10
      --save 60 10000
      --requirepass redis_password_2024
      --masterauth redis_password_2024
      --maxmemory 512mb
      --maxmemory-policy allkeys-lru
      --dir /data"

echo "✅ Redis Master-1 and Master-2 already configured with full persistence"
echo "⚠️  You need to manually add persistence settings to:"
echo "   - redis-master-3"
echo "   - redis-slave-1"
echo "   - redis-slave-2"
echo "   - redis-slave-3"
echo ""
echo "Add these lines after '--appendonly yes':"
echo "$PERSISTENCE_CONFIG"
