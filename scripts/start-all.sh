#!/bin/bash

# Start All Services Script
# This script starts Consul, Business Service, and Gateway in the correct order

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
CONSUL_HOST=${CONSUL_HOST:-localhost}
CONSUL_PORT=${CONSUL_PORT:-8500}
BUSINESS_PORT=${BUSINESS_PORT:-8081}
GATEWAY_PORT=${GATEWAY_PORT:-8080}

echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}  Starting Neobrutalism CRM Services${NC}"
echo -e "${BLUE}=================================================${NC}"
echo ""

# Function to check if port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        return 0  # Port is in use
    else
        return 1  # Port is free
    fi
}

# Function to wait for service to be healthy
wait_for_health() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=0

    echo -e "${YELLOW}Waiting for $service_name to be healthy...${NC}"

    while [ $attempt -lt $max_attempts ]; do
        if curl -f -s $url > /dev/null 2>&1; then
            echo -e "${GREEN}✓ $service_name is healthy!${NC}"
            return 0
        fi

        attempt=$((attempt + 1))
        echo -n "."
        sleep 2
    done

    echo -e "${RED}✗ $service_name health check timeout${NC}"
    return 1
}

# Step 1: Check and start Consul
echo -e "${BLUE}[1/4] Checking Consul...${NC}"

if check_port $CONSUL_PORT; then
    echo -e "${GREEN}✓ Consul is already running on port $CONSUL_PORT${NC}"
else
    echo -e "${YELLOW}Starting Consul in dev mode...${NC}"

    # Check if Consul is installed
    if ! command -v consul &> /dev/null; then
        echo -e "${RED}✗ Consul is not installed!${NC}"
        echo -e "${YELLOW}Please install Consul:${NC}"
        echo -e "  macOS: brew install consul"
        echo -e "  Windows: choco install consul"
        echo -e "  Docker: docker run -d --name=consul -p 8500:8500 consul agent -dev -ui -client=0.0.0.0"
        exit 1
    fi

    # Start Consul in background
    nohup consul agent -dev -ui > logs/consul.log 2>&1 &
    CONSUL_PID=$!
    echo "Consul PID: $CONSUL_PID"

    # Wait for Consul to be ready
    sleep 5

    if wait_for_health "http://$CONSUL_HOST:$CONSUL_PORT/v1/status/leader" "Consul"; then
        echo -e "${GREEN}✓ Consul UI: http://$CONSUL_HOST:$CONSUL_PORT/ui${NC}"
    else
        echo -e "${RED}✗ Failed to start Consul${NC}"
        exit 1
    fi
fi

echo ""

# Step 2: Check Redis (optional)
echo -e "${BLUE}[2/4] Checking Redis...${NC}"

if check_port 6379; then
    echo -e "${GREEN}✓ Redis is running on port 6379${NC}"
else
    echo -e "${YELLOW}⚠ Redis is not running (optional, L2 cache will be disabled)${NC}"
    echo -e "${YELLOW}  To start Redis: docker run -d -p 6379:6379 redis${NC}"
fi

echo ""

# Step 3: Start Business Service
echo -e "${BLUE}[3/4] Starting Business Service...${NC}"

if check_port $BUSINESS_PORT; then
    echo -e "${YELLOW}⚠ Business Service is already running on port $BUSINESS_PORT${NC}"
else
    cd business-service

    echo -e "${YELLOW}Building Business Service...${NC}"
    ./mvnw clean package -DskipTests > ../logs/business-build.log 2>&1

    echo -e "${YELLOW}Starting Business Service...${NC}"
    nohup ./mvnw spring-boot:run \
        -Dspring-boot.run.arguments="--server.port=$BUSINESS_PORT --spring.cloud.consul.host=$CONSUL_HOST --spring.cloud.consul.port=$CONSUL_PORT" \
        > ../logs/business-service.log 2>&1 &
    BUSINESS_PID=$!
    echo "Business Service PID: $BUSINESS_PID"

    cd ..

    if wait_for_health "http://localhost:$BUSINESS_PORT/actuator/health" "Business Service"; then
        echo -e "${GREEN}✓ Business Service API: http://localhost:$BUSINESS_PORT/swagger-ui.html${NC}"
    else
        echo -e "${RED}✗ Failed to start Business Service${NC}"
        echo -e "${YELLOW}Check logs: tail -f logs/business-service.log${NC}"
        exit 1
    fi
fi

echo ""

# Step 4: Start Gateway
echo -e "${BLUE}[4/4] Starting Gateway Service...${NC}"

if check_port $GATEWAY_PORT; then
    echo -e "${YELLOW}⚠ Gateway is already running on port $GATEWAY_PORT${NC}"
else
    cd gateway-service

    echo -e "${YELLOW}Building Gateway...${NC}"
    ./mvnw clean package -DskipTests > ../logs/gateway-build.log 2>&1

    echo -e "${YELLOW}Starting Gateway...${NC}"
    nohup ./mvnw spring-boot:run \
        -Dspring-boot.run.arguments="--server.port=$GATEWAY_PORT --spring.cloud.consul.host=$CONSUL_HOST --spring.cloud.consul.port=$CONSUL_PORT" \
        > ../logs/gateway-service.log 2>&1 &
    GATEWAY_PID=$!
    echo "Gateway PID: $GATEWAY_PID"

    cd ..

    if wait_for_health "http://localhost:$GATEWAY_PORT/actuator/health" "Gateway"; then
        echo -e "${GREEN}✓ Gateway API: http://localhost:$GATEWAY_PORT${NC}"
    else
        echo -e "${RED}✗ Failed to start Gateway${NC}"
        echo -e "${YELLOW}Check logs: tail -f logs/gateway-service.log${NC}"
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}=================================================${NC}"
echo -e "${GREEN}  All Services Started Successfully! 🚀${NC}"
echo -e "${GREEN}=================================================${NC}"
echo ""
echo -e "${BLUE}Service URLs:${NC}"
echo -e "  Consul UI:        ${GREEN}http://localhost:$CONSUL_PORT/ui${NC}"
echo -e "  Gateway:          ${GREEN}http://localhost:$GATEWAY_PORT${NC}"
echo -e "  Business Service: ${GREEN}http://localhost:$BUSINESS_PORT${NC}"
echo -e "  Swagger UI:       ${GREEN}http://localhost:$BUSINESS_PORT/swagger-ui.html${NC}"
echo ""
echo -e "${BLUE}Health Checks:${NC}"
echo -e "  Gateway:  ${YELLOW}curl http://localhost:$GATEWAY_PORT/actuator/health${NC}"
echo -e "  Business: ${YELLOW}curl http://localhost:$BUSINESS_PORT/actuator/health${NC}"
echo ""
echo -e "${BLUE}Service Discovery:${NC}"
echo -e "  ${YELLOW}curl http://localhost:$CONSUL_PORT/v1/catalog/services${NC}"
echo ""
echo -e "${BLUE}Logs:${NC}"
echo -e "  Consul:   ${YELLOW}tail -f logs/consul.log${NC}"
echo -e "  Gateway:  ${YELLOW}tail -f logs/gateway-service.log${NC}"
echo -e "  Business: ${YELLOW}tail -f logs/business-service.log${NC}"
echo ""
echo -e "${BLUE}To stop all services:${NC}"
echo -e "  ${YELLOW}./scripts/stop-all.sh${NC}"
echo ""
