#!/bin/bash

# Check Services Health Script

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}  Service Health Check${NC}"
echo -e "${BLUE}=================================================${NC}"
echo ""

# Function to check service
check_service() {
    local url=$1
    local name=$2

    echo -n -e "${YELLOW}Checking $name...${NC} "

    response=$(curl -s -w "%{http_code}" -o /dev/null $url 2>/dev/null)

    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✓ Healthy${NC}"
        return 0
    else
        echo -e "${RED}✗ Unhealthy (HTTP $response)${NC}"
        return 1
    fi
}

# Check Consul
check_service "http://localhost:8500/v1/status/leader" "Consul"

# Check Business Service
check_service "http://localhost:8081/actuator/health" "Business Service"

# Check Gateway
check_service "http://localhost:8080/actuator/health" "Gateway"

echo ""
echo -e "${BLUE}Service Discovery:${NC}"

# Check registered services in Consul
services=$(curl -s http://localhost:8500/v1/catalog/services 2>/dev/null)

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Services registered in Consul:${NC}"
    echo $services | jq '.' 2>/dev/null || echo $services
else
    echo -e "${RED}✗ Could not query Consul${NC}"
fi

echo ""
echo -e "${BLUE}Gateway Routes:${NC}"

# Check gateway routes
routes=$(curl -s http://localhost:8080/actuator/gateway/routes 2>/dev/null)

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Configured routes:${NC}"
    echo $routes | jq '.[] | {id: .route_id, uri: .uri}' 2>/dev/null || echo $routes
else
    echo -e "${RED}✗ Could not query Gateway routes${NC}"
fi

echo ""
