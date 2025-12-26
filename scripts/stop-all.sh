#!/bin/bash

# Stop All Services Script

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}  Stopping All Services${NC}"
echo -e "${BLUE}=================================================${NC}"
echo ""

# Function to stop service by port
stop_by_port() {
    local port=$1
    local service_name=$2

    echo -e "${YELLOW}Stopping $service_name (port $port)...${NC}"

    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        local pid=$(lsof -Pi :$port -sTCP:LISTEN -t)
        kill -15 $pid 2>/dev/null || true
        sleep 2

        # Force kill if still running
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
            kill -9 $pid 2>/dev/null || true
        fi

        echo -e "${GREEN}✓ $service_name stopped${NC}"
    else
        echo -e "${YELLOW}⚠ $service_name was not running${NC}"
    fi
}

# Stop services in reverse order
stop_by_port 8080 "Gateway"
stop_by_port 8081 "Business Service"
stop_by_port 8500 "Consul"
stop_by_port 6379 "Redis (if started by script)"

echo ""
echo -e "${GREEN}=================================================${NC}"
echo -e "${GREEN}  All Services Stopped${NC}"
echo -e "${GREEN}=================================================${NC}"
echo ""
