#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# API Gateway URL
GATEWAY_URL="http://localhost:8080"
# Or direct Booking Service URL
# GATEWAY_URL="http://localhost:8082"

echo -e "${BLUE}🧪 Testing Booking Service API${NC}"
echo -e "${BLUE}================================${NC}\n"

# Function to make API calls and display results
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4

    echo -e "${YELLOW}$description${NC}"
    echo -e "Endpoint: $method $endpoint"

    if [ -n "$data" ]; then
        echo -e "Data: $data"
        response=$(curl -s -w "|HTTP_STATUS:%{http_code}" -X $method "$GATEWAY_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "X-Correlation-ID: test-$(date +%s)" \
            -d "$data")
    else
        response=$(curl -s -w "|HTTP_STATUS:%{http_code}" -X $method "$GATEWAY_URL$endpoint")
    fi

    # Extract HTTP status code
    http_code=$(echo "$response" | grep -o 'HTTP_STATUS:[0-9]*' | cut -d':' -f2)
    # Extract response body
    body=$(echo "$response" | sed 's/|HTTP_STATUS:[0-9]*//')

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "${GREEN}Status: $http_code${NC}"
        echo -e "Response: $body\n"
    else
        echo -e "${RED}Status: $http_code${NC}"
        echo -e "Response: $body\n"
    fi
}

# Function to wait for user input
press_enter() {
    echo -e "${BLUE}Press Enter to continue...${NC}"
    read -r
}

echo -e "${BLUE}2. 🧪 Test Endpoints${NC}"
make_request "GET" "/api/test/status" "" "Test status endpoint"
make_request "GET" "/api/test/hello" "" "Test hello endpoint"

press_enter

echo -e "${BLUE}3. 👤 User Registration${NC}"
make_request "POST" "/api/auth/register" '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "firstName": "John",
    "lastName": "Doe"
}' "Register new user"

make_request "POST" "/api/auth/register" '{
    "username": "alice",
    "password": "alice123",
    "email": "alice@example.com",
    "firstName": "Alice",
    "lastName": "Smith"
}' "Register another user"

press_enter

echo -e "${BLUE}4. 🔐 User Login${NC}"
make_request "POST" "/api/auth/login" '{
    "username": "testuser",
    "password": "password123"
}' "Login with correct credentials"

make_request "POST" "/api/auth/login" '{
    "username": "testuser",
    "password": "wrongpassword"
}' "Login with wrong credentials"

press_enter

echo -e "${BLUE}5. 🏨 Create Bookings${NC}"
make_request "POST" "/api/bookings" '{
    "userId": 1,
    "roomId": 101,
    "startDate": "2024-10-25",
    "endDate": "2024-10-27",
    "correlationId": "booking-test-001"
}' "Create booking for room 101"

make_request "POST" "/api/bookings" '{
    "userId": 2,
    "roomId": 102,
    "startDate": "2024-10-28",
    "endDate": "2024-10-30",
    "correlationId": "booking-test-002"
}' "Create booking for room 102"

press_enter

echo -e "${BLUE}6. 📋 Get Bookings${NC}"
make_request "GET" "/api/bookings/user/1" "" "Get bookings for user 1"
make_request "GET" "/api/bookings/user/2" "" "Get bookings for user 2"
make_request "GET" "/api/bookings" "" "Get all bookings"

press_enter

echo -e "${BLUE}7. 🔍 Get Specific Booking${NC}"
make_request "GET" "/api/bookings/1" "" "Get booking with ID 1"
make_request "GET" "/api/bookings/2" "" "Get booking with ID 2"

press_enter

echo -e "${BLUE}8. 📊 Get Bookings by Status${NC}"
make_request "GET" "/api/bookings/status/CONFIRMED" "" "Get CONFIRMED bookings"
make_request "GET" "/api/bookings/status/PENDING" "" "Get PENDING bookings"

press_enter

echo -e "${BLUE}9. 🏨 Check Room Availability${NC}"
make_request "GET" "/api/bookings/room/101/availability?startDate=2024-10-25&endDate=2024-10-27" "" "Check availability for room 101"
make_request "GET" "/api/bookings/room/102/availability?startDate=2024-11-01&endDate=2024-11-03" "" "Check availability for room 102"

press_enter

echo -e "${BLUE}10. ❌ Cancel Booking${NC}"
make_request "POST" "/api/bookings/1/cancel" "" "Cancel booking 1"

press_enter

echo -e "${BLUE}11. 🔄 Check Status After Cancellation${NC}"
make_request "GET" "/api/bookings/status/CANCELLED" "" "Get CANCELLED bookings"
make_request "GET" "/api/bookings/user/1" "" "Get updated bookings for user 1"

press_enter

echo -e "${BLUE}12. 🐛 Error Handling Tests${NC}"
make_request "GET" "/api/bookings/999" "" "Get non-existent booking"
make_request "POST" "/api/bookings" '{
    "userId": 999,
    "roomId": 999,
    "startDate": "2024-10-25",
    "endDate": "2024-10-27"
}' "Create booking with non-existent user"
make_request "POST" "/api/bookings" '{
    "userId": 1,
    "roomId": 101,
    "startDate": "2024-10-20",
    "endDate": "2024-10-19"
}' "Create booking with invalid dates"

echo -e "${GREEN}🎉 Тестирование Booking Service завершено!${NC}"