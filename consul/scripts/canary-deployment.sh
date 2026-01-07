#!/bin/bash

# Canary Deployment Script
# Gradually shift traffic from stable to canary version

set -e

CONSUL_HTTP_ADDR="${CONSUL_HTTP_ADDR:-http://localhost:8500}"
SERVICE_NAME="${1:-business-service}"
CANARY_PERCENTAGE="${2:-10}"

echo "========================================="
echo "Canary Deployment Manager"
echo "========================================="
echo ""
echo "Service: $SERVICE_NAME"
echo "Canary Traffic: ${CANARY_PERCENTAGE}%"
echo ""

# Validate percentage
if [ "$CANARY_PERCENTAGE" -lt 0 ] || [ "$CANARY_PERCENTAGE" -gt 100 ]; then
    echo "❌ Error: Canary percentage must be between 0 and 100"
    exit 1
fi

STABLE_PERCENTAGE=$((100 - CANARY_PERCENTAGE))

# Create service splitter configuration
SPLITTER_CONFIG=$(cat <<EOF
{
  "Kind": "service-splitter",
  "Name": "$SERVICE_NAME",
  "Splits": [
    {
      "Weight": $STABLE_PERCENTAGE,
      "ServiceSubset": "stable"
    },
    {
      "Weight": $CANARY_PERCENTAGE,
      "ServiceSubset": "canary"
    }
  ],
  "Meta": {
    "deployment_strategy": "canary",
    "canary_percentage": "$CANARY_PERCENTAGE",
    "last_updated": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  }
}
EOF
)

echo "📝 Updating traffic split configuration..."
echo "$SPLITTER_CONFIG" | curl -sf -X PUT "${CONSUL_HTTP_ADDR}/v1/config" -d @-

if [ $? -eq 0 ]; then
    echo "✅ Traffic split updated successfully!"
    echo ""
    echo "Current traffic distribution:"
    echo "  - Stable version: ${STABLE_PERCENTAGE}%"
    echo "  - Canary version: ${CANARY_PERCENTAGE}%"
else
    echo "❌ Failed to update traffic split"
    exit 1
fi

echo ""
echo "📊 Monitoring recommendations:"
echo "  1. Watch error rates: http://localhost:3000"
echo "  2. Check response times in Grafana"
echo "  3. Review traces in Jaeger: http://localhost:16686"
echo ""

# Suggested next steps
if [ "$CANARY_PERCENTAGE" -eq 0 ]; then
    echo "💡 Canary deployment rolled back to stable"
elif [ "$CANARY_PERCENTAGE" -eq 100 ]; then
    echo "💡 Canary deployment fully promoted!"
    echo "   Consider updating service-resolver to make canary the new stable"
elif [ "$CANARY_PERCENTAGE" -lt 50 ]; then
    echo "💡 Next step: Increase canary traffic to $((CANARY_PERCENTAGE + 10))%"
    echo "   Run: $0 $SERVICE_NAME $((CANARY_PERCENTAGE + 10))"
else
    echo "💡 Canary looks good? Promote to 100% and mark as stable"
fi
echo ""
