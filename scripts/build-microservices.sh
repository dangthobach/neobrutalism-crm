#!/bin/bash

# Build script for all microservices
# This script compiles and packages all microservices in the correct order

set -e  # Exit on error

echo "========================================="
echo "Building Neobrutalism CRM Microservices"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}➜ $1${NC}"
}

# Build counter
SUCCESS_COUNT=0
TOTAL_SERVICES=2

# Function to build a service
build_service() {
    local service_name=$1
    local service_path=$2

    print_info "Building $service_name..."

    if cd "$service_path" && mvn clean package -DskipTests; then
        print_success "$service_name built successfully"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        cd - > /dev/null
        return 0
    else
        print_error "$service_name build failed"
        cd - > /dev/null
        return 1
    fi
}

# Main build process
echo "Starting build process..."
echo ""

# Build Gateway Service
build_service "API Gateway" "microservices/gateway"
echo ""

# Build IAM Service
build_service "IAM Service" "microservices/iam-service"
echo ""

# Summary
echo "========================================="
echo "Build Summary"
echo "========================================="
echo "Services built: $SUCCESS_COUNT/$TOTAL_SERVICES"

if [ $SUCCESS_COUNT -eq $TOTAL_SERVICES ]; then
    print_success "All microservices built successfully!"
    echo ""
    echo "Generated artifacts:"
    ls -lh microservices/*/target/*.jar | grep -v ".original"
    exit 0
else
    print_error "Some microservices failed to build"
    exit 1
fi
