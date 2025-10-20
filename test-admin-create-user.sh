#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

API_URL="${1:-http://localhost:8080}"
BASE_PATH="/api/user"

echo -e "${BLUE}🧪 Testing Admin Create User Endpoint${NC}"
echo -e "${BLUE}=====================================${NC}"
echo -e "Using API URL: $API_URL$BASE_PATH\n"

# Function to make API calls and display results
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4

    echo -e "${YELLOW}➡️  $description${NC}"
    echo "Endpoint: $method $endpoint"
    echo "Data: $data"

    response=$(curl -s -w "|HTTP_STATUS:%{http_code}" -X $method "$API_URL$endpoint" \
        -H "Content-Type: application/json" \
        -d "$data")

    http_code=$(echo "$response" | grep -o 'HTTP_STATUS:[0-9]*' | cut -d':' -f2)
    body=$(echo "$response" | sed 's/|HTTP_STATUS:[0-9]*//')

    echo -e "Response: $body"
    echo -e "Status: $http_code\n"
}

echo -e "${BLUE}=== Testing ADMIN Create User Endpoint ===${NC}"

echo -e "${YELLOW}1. Creating User via ADMIN Endpoint${NC}"
make_request "POST" "$BASE_PATH" '{
    "username": "admin_created_user1",
    "password": "adminpass123",
    "email": "admin.created1@example.com",
    "firstName": "AdminCreated",
    "lastName": "UserOne"
}' "Create user via admin endpoint"

echo -e "${YELLOW}2. Creating Another User via ADMIN Endpoint${NC}"
make_request "POST" "$BASE_PATH" '{
    "username": "admin_created_user2",
    "password": "adminpass456",
    "email": "admin.created2@example.com",
    "firstName": "AdminCreated",
    "lastName": "UserTwo"
}' "Create another user via admin endpoint"

echo -e "${YELLOW}3. Creating User with Minimal Data${NC}"
make_request "POST" "$BASE_PATH" '{
    "username": "minimal_admin_user",
    "password": "minimal123"
}' "Create user with minimal data via admin endpoint"

echo -e "${YELLOW}4. Testing Error - Duplicate Username${NC}"
make_request "POST" "$BASE_PATH" '{
    "username": "admin_created_user1",
    "password": "differentpass",
    "email": "different@example.com"
}' "Try to create user with duplicate username"

echo -e "${YELLOW}5. Verifying Created Users${NC}"
make_request "GET" "$BASE_PATH" "" "Get all users to verify creation"

echo -e "${BLUE}=== Testing Completed ===${NC}"
echo -e "${GREEN}✅ Admin create user testing completed!${NC}"