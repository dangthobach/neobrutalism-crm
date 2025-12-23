#!/bin/bash

# Import Keycloak Realm Configuration
# This script imports the CRM realm into Keycloak

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
REALM_FILE="$PROJECT_ROOT/docker/keycloak/realm-import.json"
KEYCLOAK_URL="http://localhost:8180"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Wait for Keycloak to be ready
wait_for_keycloak() {
    log_info "Waiting for Keycloak to be ready..."

    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -sf "$KEYCLOAK_URL/health/ready" > /dev/null 2>&1; then
            log_success "Keycloak is ready"
            return 0
        fi

        log_info "Attempt $attempt/$max_attempts - Keycloak not ready yet..."
        sleep 10
        attempt=$((attempt + 1))
    done

    log_error "Keycloak failed to start after $max_attempts attempts"
    return 1
}

# Get admin access token
get_admin_token() {
    log_info "Getting admin access token..."

    local token=$(curl -sf -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=admin" \
        -d "password=admin" \
        -d "grant_type=password" \
        -d "client_id=admin-cli" \
        | jq -r '.access_token')

    if [ -z "$token" ] || [ "$token" = "null" ]; then
        log_error "Failed to get admin token"
        return 1
    fi

    echo "$token"
}

# Check if realm exists
realm_exists() {
    local token=$1

    local realms=$(curl -sf "$KEYCLOAK_URL/admin/realms" \
        -H "Authorization: Bearer $token" \
        | jq -r '.[].realm')

    if echo "$realms" | grep -q "^crm$"; then
        return 0
    else
        return 1
    fi
}

# Delete existing realm
delete_realm() {
    local token=$1

    log_info "Deleting existing 'crm' realm..."

    curl -sf -X DELETE "$KEYCLOAK_URL/admin/realms/crm" \
        -H "Authorization: Bearer $token"

    log_success "Existing realm deleted"
}

# Import realm
import_realm() {
    local token=$1

    log_info "Importing 'crm' realm from $REALM_FILE..."

    local response=$(curl -sf -X POST "$KEYCLOAK_URL/admin/realms" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d @"$REALM_FILE")

    if [ $? -eq 0 ]; then
        log_success "Realm imported successfully"
        return 0
    else
        log_error "Failed to import realm"
        return 1
    fi
}

# Verify realm
verify_realm() {
    local token=$1

    log_info "Verifying realm import..."

    # Check realm
    local realm=$(curl -sf "$KEYCLOAK_URL/admin/realms/crm" \
        -H "Authorization: Bearer $token" \
        | jq -r '.realm')

    if [ "$realm" = "crm" ]; then
        log_success "Realm 'crm' verified"
    else
        log_error "Realm verification failed"
        return 1
    fi

    # Check clients
    local clients=$(curl -sf "$KEYCLOAK_URL/admin/realms/crm/clients" \
        -H "Authorization: Bearer $token" \
        | jq -r '.[].clientId')

    log_info "Clients found:"
    echo "$clients" | while read -r client; do
        echo "  - $client"
    done

    # Check users
    local users=$(curl -sf "$KEYCLOAK_URL/admin/realms/crm/users" \
        -H "Authorization: Bearer $token" \
        | jq -r '.[].username')

    log_info "Users found:"
    echo "$users" | while read -r user; do
        echo "  - $user"
    done

    log_success "Realm verification complete"
}

# Test authentication
test_authentication() {
    log_info "Testing authentication..."

    # Test with admin user
    local token=$(curl -sf -X POST "$KEYCLOAK_URL/realms/crm/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=admin@crm.local" \
        -d "password=admin123" \
        -d "grant_type=password" \
        -d "client_id=crm-frontend" \
        | jq -r '.access_token')

    if [ -n "$token" ] && [ "$token" != "null" ]; then
        log_success "Authentication test successful"

        # Decode token to show claims
        log_info "Token claims:"
        echo "$token" | cut -d. -f2 | base64 -d 2>/dev/null | jq '.' || true
    else
        log_error "Authentication test failed"
        return 1
    fi
}

# Main function
main() {
    log_info "Starting Keycloak realm import..."

    # Wait for Keycloak
    if ! wait_for_keycloak; then
        exit 1
    fi

    # Get admin token
    local admin_token=$(get_admin_token)
    if [ -z "$admin_token" ]; then
        exit 1
    fi

    # Check if realm exists
    if realm_exists "$admin_token"; then
        log_info "Realm 'crm' already exists"

        read -p "Delete and reimport? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            delete_realm "$admin_token"

            # Get new token after realm deletion
            admin_token=$(get_admin_token)
        else
            log_info "Skipping import"
            exit 0
        fi
    fi

    # Import realm
    if import_realm "$admin_token"; then
        # Get new token for verification
        admin_token=$(get_admin_token)

        # Verify import
        if verify_realm "$admin_token"; then
            # Test authentication
            test_authentication

            log_success "Keycloak realm import complete!"
            echo ""
            echo "Realm: crm"
            echo "URL: $KEYCLOAK_URL/realms/crm"
            echo "Admin Console: $KEYCLOAK_URL/admin"
            echo ""
            echo "Test Users:"
            echo "  - admin@crm.local / admin123 (Super Admin)"
            echo "  - tenant1-admin@crm.local / admin123 (Tenant Admin)"
            echo "  - user1@tenant1.com / user123 (User)"
            echo "  - instructor1@tenant1.com / instructor123 (Instructor)"
            echo "  - student1@tenant1.com / student123 (Student)"
        else
            log_error "Realm import verification failed"
            exit 1
        fi
    else
        log_error "Realm import failed"
        exit 1
    fi
}

main "$@"
