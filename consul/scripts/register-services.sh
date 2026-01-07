#!/bin/bash

# Script to register services with Consul and configure Service Mesh
# This script registers all service definitions and configurations

set -e

CONSUL_HTTP_ADDR="${CONSUL_HTTP_ADDR:-http://localhost:8500}"

echo "========================================="
echo "Consul Service Mesh Registration Script"
echo "========================================="
echo ""

# Wait for Consul to be ready
echo "⏳ Waiting for Consul server to be ready..."
until curl -sf "${CONSUL_HTTP_ADDR}/v1/status/leader" | grep -q .; do
  echo "  Consul not ready yet, waiting..."
  sleep 2
done
echo "✅ Consul server is ready"
echo ""

# Register service definitions
echo "📝 Registering service definitions..."

echo "  → Registering gateway-service..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/agent/service/register" \
  --data @../config/gateway-service.json
echo "    ✅ Gateway service registered"

echo "  → Registering business-service..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/agent/service/register" \
  --data @../config/business-service.json
echo "    ✅ Business service registered"

echo "  → Registering iam-service..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/agent/service/register" \
  --data @../config/iam-service.json
echo "    ✅ IAM service registered"

echo ""

# Configure service defaults
echo "⚙️  Configuring service defaults..."

echo "  → Configuring proxy defaults..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/proxy-defaults.json
echo "    ✅ Proxy defaults configured"

echo "  → Configuring gateway-service defaults..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/service-defaults/gateway-defaults.json
echo "    ✅ Gateway defaults configured"

echo "  → Configuring business-service defaults..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/service-defaults/business-defaults.json
echo "    ✅ Business defaults configured"

echo "  → Configuring iam-service defaults..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/service-defaults/iam-defaults.json
echo "    ✅ IAM defaults configured"

echo ""

# Configure service intentions (security policies)
echo "🔒 Configuring service intentions (mTLS policies)..."

echo "  → Gateway → Business intention..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/intentions/gateway-to-business.json
echo "    ✅ Gateway to Business intention configured"

echo "  → Gateway → IAM intention..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/intentions/gateway-to-iam.json
echo "    ✅ Gateway to IAM intention configured"

echo "  → Business → IAM intention..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/intentions/business-to-iam.json
echo "    ✅ Business to IAM intention configured"

echo ""

# Configure traffic management
echo "🚦 Configuring traffic management..."

echo "  → Service router for business-service..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/traffic-management/business-service-router.json
echo "    ✅ Service router configured"

echo "  → Service splitter for canary deployments..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/traffic-management/business-service-splitter.json
echo "    ✅ Service splitter configured"

echo "  → Service resolver for load balancing..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/traffic-management/business-service-resolver.json
echo "    ✅ Service resolver configured"

echo ""

# Configure resilience policies
echo "🛡️  Configuring resilience policies..."

echo "  → Circuit breaker configuration..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/resilience/circuit-breaker-config.json
echo "    ✅ Circuit breaker configured"

echo "  → Timeout configuration..."
curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" \
  --data @../config/resilience/timeout-config.json
echo "    ✅ Timeout policies configured"

echo ""

# Verify configuration
echo "🔍 Verifying configuration..."

echo "  → Checking registered services..."
SERVICES=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/agent/services" | jq -r 'keys[]')
echo "    Registered services:"
echo "$SERVICES" | sed 's/^/      - /'

echo ""
echo "  → Checking service mesh configuration..."
CONFIG_ENTRIES=$(curl -sf "${CONSUL_HTTP_ADDR}/v1/config" | jq -r '.[] | .Kind + "/" + .Name')
echo "    Configuration entries:"
echo "$CONFIG_ENTRIES" | sed 's/^/      - /'

echo ""
echo "========================================="
echo "✅ Service Mesh registration completed!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. Start your microservices: docker-compose -f docker-compose.service-mesh.yml up -d"
echo "  2. View Consul UI: http://localhost:8500"
echo "  3. View Grafana: http://localhost:3000 (admin/admin123)"
echo "  4. View Jaeger: http://localhost:16686"
echo "  5. View Prometheus: http://localhost:9090"
echo ""
