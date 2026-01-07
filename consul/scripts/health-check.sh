#!/bin/bash

# Service Mesh Health Check Script
# Checks the health of all services and mesh components

CONSUL_HTTP_ADDR="${CONSUL_HTTP_ADDR:-http://localhost:8500}"

echo "========================================="
echo "Service Mesh Health Check"
echo "========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Consul Server
echo "🔍 Checking Consul Server..."
if curl -sf "${CONSUL_HTTP_ADDR}/v1/status/leader" | grep -q .; then
    echo -e "  ${GREEN}✅ Consul server is running${NC}"
else
    echo -e "  ${RED}❌ Consul server is not accessible${NC}"
    exit 1
fi
echo ""

# Check all registered services
echo "🔍 Checking Registered Services..."
SERVICES=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/agent/services" | jq -r 'keys[]')

for service in $SERVICES; do
    # Skip sidecar proxies
    if [[ "$service" == *"-sidecar-proxy" ]]; then
        continue
    fi

    echo "  Service: $service"

    # Get service health
    HEALTH=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/health/service/${service}" | jq -r '.[0].Checks[].Status' 2>/dev/null)

    if echo "$HEALTH" | grep -q "passing"; then
        echo -e "    ${GREEN}✅ Status: Healthy${NC}"
    elif echo "$HEALTH" | grep -q "warning"; then
        echo -e "    ${YELLOW}⚠️  Status: Warning${NC}"
    else
        echo -e "    ${RED}❌ Status: Critical or Unknown${NC}"
    fi

    # Get service instances
    INSTANCES=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/catalog/service/${service}" | jq -r 'length')
    echo "    Instances: $INSTANCES"

done
echo ""

# Check Service Mesh Connect
echo "🔍 Checking Service Mesh (Consul Connect)..."
CONNECT_ENABLED=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/agent/self" | jq -r '.DebugConfig.ConnectEnabled')
if [ "$CONNECT_ENABLED" = "true" ]; then
    echo -e "  ${GREEN}✅ Consul Connect is enabled${NC}"
else
    echo -e "  ${RED}❌ Consul Connect is not enabled${NC}"
fi
echo ""

# Check Envoy Sidecars
echo "🔍 Checking Envoy Sidecar Proxies..."
SIDECAR_SERVICES=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/agent/services" | jq -r 'keys[] | select(endswith("-sidecar-proxy"))')

for sidecar in $SIDECAR_SERVICES; do
    echo "  Sidecar: $sidecar"
    HEALTH=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/health/service/${sidecar}" | jq -r '.[0].Checks[].Status' 2>/dev/null)

    if echo "$HEALTH" | grep -q "passing"; then
        echo -e "    ${GREEN}✅ Status: Healthy${NC}"
    else
        echo -e "    ${RED}❌ Status: Unhealthy${NC}"
    fi
done
echo ""

# Check Service Intentions
echo "🔍 Checking Service Intentions..."
INTENTIONS=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/connect/intentions" | jq -r 'length')
echo "  Total intentions configured: $INTENTIONS"
if [ "$INTENTIONS" -gt 0 ]; then
    echo -e "  ${GREEN}✅ Service intentions are configured${NC}"
else
    echo -e "  ${YELLOW}⚠️  No service intentions configured${NC}"
fi
echo ""

# Check Configuration Entries
echo "🔍 Checking Configuration Entries..."
CONFIG_KINDS=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/config" | jq -r 'group_by(.Kind) | map({kind: .[0].Kind, count: length}) | .[]')
echo "  Configuration entries by kind:"
echo "$CONFIG_KINDS" | jq -r '"    - " + .kind + ": " + (.count | tostring)'
echo ""

# Check Observability Stack
echo "🔍 Checking Observability Stack..."

# Prometheus
if curl -sf http://localhost:9090/-/healthy > /dev/null 2>&1; then
    echo -e "  ${GREEN}✅ Prometheus is healthy${NC}"
else
    echo -e "  ${YELLOW}⚠️  Prometheus is not accessible${NC}"
fi

# Grafana
if curl -sf http://localhost:3000/api/health > /dev/null 2>&1; then
    echo -e "  ${GREEN}✅ Grafana is healthy${NC}"
else
    echo -e "  ${YELLOW}⚠️  Grafana is not accessible${NC}"
fi

# Jaeger
if curl -sf http://localhost:14269/ > /dev/null 2>&1; then
    echo -e "  ${GREEN}✅ Jaeger is healthy${NC}"
else
    echo -e "  ${YELLOW}⚠️  Jaeger is not accessible${NC}"
fi

echo ""

# Summary
echo "========================================="
echo "Health Check Summary"
echo "========================================="
TOTAL_SERVICES=$(echo "$SERVICES" | wc -l)
HEALTHY_SERVICES=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/health/state/passing" | jq -r 'length')
echo "Total Services: $TOTAL_SERVICES"
echo "Healthy Services: $HEALTHY_SERVICES"
echo ""

if [ "$HEALTHY_SERVICES" -eq "$TOTAL_SERVICES" ]; then
    echo -e "${GREEN}✅ All services are healthy!${NC}"
else
    echo -e "${YELLOW}⚠️  Some services need attention${NC}"
fi
echo ""
