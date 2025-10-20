#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

API_URL="${1:-http://localhost:8080}"
BASE_PATH="/api/user"  # Правильный путь

echo -e "${BLUE}🧪 Testing Auth Controller API${NC}"
echo -e "${BLUE}==============================${NC}"
echo -e "Using API URL: $API_URL"
echo -e "Base Path: $BASE_PATH"
echo -e "${YELLOW}Note: Pre-created users: admin/admin123, testuser/password${NC}\n"

# Function to make API calls and display results
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    local expected_status="${5:-200}"

    echo -e "${YELLOW}➡️  $description${NC}"
    echo "Endpoint: $method $endpoint"
    if [ -n "$data" ]; then
        echo "Data: $data"
    fi

    if [ -n "$data" ]; then
        response=$(curl -s -w "|HTTP_STATUS:%{http_code}" -X $method "$API_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "|HTTP_STATUS:%{http_code}" -X $method "$API_URL$endpoint")
    fi

    http_code=$(echo "$response" | grep -o 'HTTP_STATUS:[0-9]*' | cut -d':' -f2)
    body=$(echo "$response" | sed 's/|HTTP_STATUS:[0-9]*//')

    if [ "$http_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✅ SUCCESS (Status: $http_code)${NC}"
        echo "Response: $body"
    else
        echo -e "${RED}❌ FAILED (Expected: $expected_status, Got: $http_code)${NC}"
        echo "Response: $body"
    fi
    echo
}

# Wait for user input
press_enter() {
    echo -e "${BLUE}Press Enter to continue...${NC}"
    read -r
}

echo -e "${BLue}=== TESTING PRE-CREATED USERS ===${NC}"

echo -e "${YELLOW}1. Testing Authentication with Pre-created Users${NC}"
make_request "POST" "$BASE_PATH/auth" '{
    "username": "admin",
    "password": "admin123"
}' "Login as ADMIN user"

make_request "POST" "$BASE_PATH/auth" '{
    "username": "testuser",
    "password": "password"
}' "Login as TESTUSER"

make_request "POST" "$BASE_PATH/auth" '{
    "username": "admin",
    "password": "wrongpassword"
}' "Login admin with wrong password" 400

press_enter

echo -e "${BLUE}=== PUBLIC ENDPOINTS (No auth required) ===${NC}"

echo -e "${YELLOW}2. Testing New User Registration${NC}"
make_request "POST" "$BASE_PATH/register" '{
    "username": "newuser1",
    "password": "newpass123",
    "email": "newuser1@example.com",
    "firstName": "New",
    "lastName": "UserOne"
}' "Register new user 1"

make_request "POST" "$BASE_PATH/register" '{
    "username": "newuser2",
    "password": "newpass456",
    "email": "newuser2@example.com",
    "firstName": "New",
    "lastName": "UserTwo"
}' "Register new user 2"

make_request "POST" "$BASE_PATH/register" '{
    "username": "simpleuser",
    "password": "simple123"
}' "Register user with minimal data"

press_enter

echo -e "${YELLOW}3. Testing Authentication with New Users${NC}"
make_request "POST" "$BASE_PATH/auth" '{
    "username": "newuser1",
    "password": "newpass123"
}' "Login new user 1 - correct credentials"

make_request "POST" "$BASE_PATH/auth" '{
    "username": "newuser1",
    "password": "wrongpassword"
}' "Login new user 1 - wrong password" 400

press_enter

echo -e "${YELLOW}4. Testing Error Cases${NC}"
make_request "POST" "$BASE_PATH/register" '{
    "password": "nousername123"
}' "Registration without username" 400

make_request "POST" "$BASE_PATH/register" '{
    "username": "nopassworduser"
}' "Registration without password" 400

make_request "POST" "$BASE_PATH/register" '{
    "username": "admin",
    "password": "differentpass"
}' "Registration with existing admin username" 400

make_request "POST" "$BASE_PATH/register" '{
    "username": "testuser",
    "password": "differentpass"
}' "Registration with existing testuser username" 400

press_enter

echo -e "${BLUE}=== ADMIN ENDPOINTS (Will fail without admin auth) ===${NC}"

echo -e "${YELLOW}5. Testing Admin Endpoints${NC}"
make_request "GET" "$BASE_PATH" "" "Get all users"

make_request "POST" "$BASE_PATH" '{
    "username": "admin_created_user",
    "password": "adminpass123",
    "email": "admin.created@example.com",
    "firstName": "AdminCreated",
    "lastName": "User"
}' "Create user via admin endpoint"

make_request "PATCH" "$BASE_PATH/3" '{
    "username": "updated_user",
    "password": "updatedpass123",
    "email": "updated@example.com",
    "firstName": "Updated",
    "lastName": "User"
}' "Update user (ID 3)"

make_request "DELETE" "$BASE_PATH?id=3" "" "Delete user (ID 3)"

press_enter

echo -e "${YELLOW}6. Testing User Profile${NC}"
make_request "GET" "$BASE_PATH/profile?username=admin" "" "Get admin profile"

make_request "GET" "$BASE_PATH/profile?username=testuser" "" "Get testuser profile"

echo -e "${BLUE}=== SUMMARY ===${NC}"
echo -e "${GREEN}✅ Pre-created users: admin/admin123, testuser/password${NC}"
echo -e "${GREEN}✅ Base path: $BASE_PATH${NC}"
echo -e "${YELLOW}📝 Note: All endpoints are under /api/user${NC}"

echo -e "\n${BLUE}=== TESTING COMPLETED ===${NC}"
echo -e "${GREEN}✅ All tests executed! Check the results above.${NC}"