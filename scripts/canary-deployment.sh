#!/bin/bash

# Canary Deployment Script for IAM Service
# Gradually increases traffic from 1% to 100%
# Usage: ./canary-deployment.sh [stage]
# Stages: init, 1, 5, 10, 25, 50, 100, rollback

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.microservices.yml"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="http://localhost:8080"
IAM_SERVICE_URL="http://localhost:8081"
PROMETHEUS_URL="http://localhost:9090"
WAIT_TIME=300  # 5 minutes between checks

# Function to print colored messages
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check service health
check_health() {
    local service_name=$1
    local health_url=$2

    log_info "Checking health of $service_name..."

    if curl -sf "$health_url/actuator/health" > /dev/null; then
        log_success "$service_name is healthy"
        return 0
    else
        log_error "$service_name is unhealthy"
        return 1
    fi
}

# Function to set canary percentage
set_canary_percentage() {
    local percentage=$1

    log_info "Setting canary percentage to $percentage%..."

    # Update environment variable
    export FEATURE_IAM_SERVICE_PERCENTAGE=$percentage

    # Restart gateway with new configuration
    log_info "Restarting Gateway with $percentage% traffic to IAM service..."
    docker-compose -f "$COMPOSE_FILE" up -d gateway

    sleep 10

    if check_health "Gateway" "$GATEWAY_URL"; then
        log_success "Gateway restarted with $percentage% canary traffic"
        return 0
    else
        log_error "Gateway failed to restart"
        return 1
    fi
}

# Function to get metrics from Prometheus
get_metrics() {
    local metric_name=$1
    local query=$2

    local result=$(curl -s "$PROMETHEUS_URL/api/v1/query?query=$query" | jq -r '.data.result[0].value[1]')
    echo "$result"
}

# Function to monitor metrics
monitor_metrics() {
    local duration=$1

    log_info "Monitoring metrics for $duration seconds..."

    local end_time=$(($(date +%s) + duration))

    while [ $(date +%s) -lt $end_time ]; do
        # Request rate
        local gateway_rate=$(get_metrics "gateway_requests" 'rate(http_server_requests_seconds_count{job="gateway"}[1m])')
        local iam_rate=$(get_metrics "iam_requests" 'rate(http_server_requests_seconds_count{job="iam-service"}[1m])')

        # Error rate
        local gateway_errors=$(get_metrics "gateway_errors" 'rate(http_server_requests_seconds_count{job="gateway",status=~"5.."}[1m])')
        local iam_errors=$(get_metrics "iam_errors" 'rate(http_server_requests_seconds_count{job="iam-service",status=~"5.."}[1m])')

        # Latency P95
        local gateway_p95=$(get_metrics "gateway_p95" 'histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="gateway"}[1m]))')
        local iam_p95=$(get_metrics "iam_p95" 'histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="iam-service"}[1m]))')

        # Cache hit rate
        local cache_hit_rate=$(get_metrics "cache_hit_rate" 'rate(cache_gets_total{result="hit"}[1m]) / rate(cache_gets_total[1m])')

        echo ""
        echo "===== Metrics Report ====="
        echo "Gateway Requests/s: ${gateway_rate:-N/A}"
        echo "IAM Service Requests/s: ${iam_rate:-N/A}"
        echo "Gateway Error Rate: ${gateway_errors:-0}"
        echo "IAM Error Rate: ${iam_errors:-0}"
        echo "Gateway P95 Latency: ${gateway_p95:-N/A}s"
        echo "IAM P95 Latency: ${iam_p95:-N/A}s"
        echo "Cache Hit Rate: ${cache_hit_rate:-N/A}"
        echo "========================="
        echo ""

        sleep 60
    done
}

# Function to verify canary deployment
verify_deployment() {
    local percentage=$1

    log_info "Verifying canary deployment at $percentage%..."

    # Check health
    if ! check_health "Gateway" "$GATEWAY_URL"; then
        return 1
    fi

    if ! check_health "IAM Service" "$IAM_SERVICE_URL"; then
        return 1
    fi

    # Monitor for 5 minutes
    monitor_metrics 300

    # Get final metrics
    local error_rate=$(get_metrics "error_rate" 'rate(http_server_requests_seconds_count{status=~"5.."}[5m])')
    local p95_latency=$(get_metrics "p95_latency" 'histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))')

    # Check thresholds
    local error_threshold=0.01  # 1% error rate
    local latency_threshold=0.5  # 500ms P95

    if (( $(echo "$error_rate > $error_threshold" | bc -l) )); then
        log_error "Error rate too high: $error_rate (threshold: $error_threshold)"
        return 1
    fi

    if (( $(echo "$p95_latency > $latency_threshold" | bc -l) )); then
        log_warning "P95 latency high: $p95_latency (threshold: $latency_threshold)"
    fi

    log_success "Canary deployment verified at $percentage%"
    return 0
}

# Function to rollback
rollback() {
    log_warning "Rolling back to monolith..."

    set_canary_percentage 0

    if check_health "Gateway" "$GATEWAY_URL"; then
        log_success "Rollback successful - all traffic routed to monolith"
    else
        log_error "Rollback failed - manual intervention required"
        exit 1
    fi
}

# Function to initialize canary deployment
init_canary() {
    log_info "Initializing canary deployment..."

    # Check prerequisites
    log_info "Checking prerequisites..."

    # Check if services are running
    if ! docker-compose -f "$COMPOSE_FILE" ps | grep -q "crm-gateway.*Up"; then
        log_error "Gateway is not running. Start with: docker-compose -f $COMPOSE_FILE up -d"
        exit 1
    fi

    if ! docker-compose -f "$COMPOSE_FILE" ps | grep -q "crm-iam-service.*Up"; then
        log_error "IAM Service is not running. Start with: docker-compose -f $COMPOSE_FILE up -d"
        exit 1
    fi

    # Check health
    check_health "Gateway" "$GATEWAY_URL" || exit 1
    check_health "IAM Service" "$IAM_SERVICE_URL" || exit 1

    # Import Keycloak realm
    log_info "Importing Keycloak realm..."
    "$SCRIPT_DIR/import-keycloak-realm.sh"

    # Import Casbin policies
    log_info "Importing Casbin policies..."
    docker-compose -f "$COMPOSE_FILE" exec -T postgres psql -U crm_user -d iam_db < "$SCRIPT_DIR/init-casbin-policies.sql"

    log_success "Canary deployment initialized"
    log_info "Ready to start with 1% traffic"
}

# Main script
main() {
    local stage=${1:-help}

    case $stage in
        init)
            init_canary
            ;;
        1|5|10|25|50|100)
            log_info "Starting canary deployment: $stage%"

            if set_canary_percentage "$stage"; then
                if verify_deployment "$stage"; then
                    log_success "Canary deployment $stage% successful"

                    if [ "$stage" -ne 100 ]; then
                        log_info "Monitor metrics for 24-48 hours before proceeding to next stage"
                        log_info "Next stage: $([ "$stage" -eq 1 ] && echo "5" || [ "$stage" -eq 5 ] && echo "10" || [ "$stage" -eq 10 ] && echo "25" || [ "$stage" -eq 25 ] && echo "50" || echo "100")%"
                    else
                        log_success "Canary deployment complete - 100% traffic on IAM service"
                    fi
                else
                    log_error "Canary deployment $stage% failed verification"

                    read -p "Rollback to previous stage? (y/n) " -n 1 -r
                    echo
                    if [[ $REPLY =~ ^[Yy]$ ]]; then
                        rollback
                    fi
                    exit 1
                fi
            else
                log_error "Failed to set canary percentage to $stage%"
                exit 1
            fi
            ;;
        rollback)
            rollback
            ;;
        status)
            log_info "Checking deployment status..."
            check_health "Gateway" "$GATEWAY_URL"
            check_health "IAM Service" "$IAM_SERVICE_URL"
            monitor_metrics 60
            ;;
        help|*)
            echo "Canary Deployment Script"
            echo ""
            echo "Usage: $0 [stage]"
            echo ""
            echo "Stages:"
            echo "  init      - Initialize canary deployment (import realm & policies)"
            echo "  1         - Set traffic to 1%"
            echo "  5         - Set traffic to 5%"
            echo "  10        - Set traffic to 10%"
            echo "  25        - Set traffic to 25%"
            echo "  50        - Set traffic to 50%"
            echo "  100       - Set traffic to 100% (complete migration)"
            echo "  rollback  - Rollback to monolith (0%)"
            echo "  status    - Check current status and metrics"
            echo ""
            echo "Example workflow:"
            echo "  $0 init      # Initialize"
            echo "  $0 1         # Start with 1%"
            echo "  # Wait 24-48 hours, monitor metrics"
            echo "  $0 5         # Increase to 5%"
            echo "  # Continue gradually..."
            echo "  $0 100       # Complete migration"
            ;;
    esac
}

main "$@"
