#!/bin/bash

# test-api.sh
# Тесты для Hotel Management API

BASE_URL="http://localhost:8080/api"
AUTH_USER="user:password"
AUTH_ADMIN="admin:password"
AUTH_INTERNAL="internal:password"

echo "=================================================="
echo "Hotel Management API Tests"
echo "=================================================="

# Функция для выполнения запросов
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local auth=$4

    echo "➡️ $method $url"

    local curl_cmd="curl -s -w ' | HTTP_STATUS:%{http_code}' -X $method '$url' -H 'Content-Type: application/json' -H 'Accept: application/json'"

    if [ ! -z "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    if [ ! -z "$auth" ]; then
        curl_cmd="$curl_cmd -u $auth"
    fi

    eval $curl_cmd
    echo ""
    echo "---"
}

# Пауза между запросами
sleep_short() {
    sleep 1
}

# 1. Тестирование отелей (USER/ADMIN)
echo "1. TESTING HOTELS ENDPOINTS"

echo "🔹 Получение всех отелей (USER)"
make_request "GET" "$BASE_URL/hotels" "" "$AUTH_USER"

sleep_short

echo "🔹 Создание отеля (ADMIN)"
HOTEL_DATA='{
    "name": "Grand Plaza Hotel",
    "address": "123 Main Street, City Center",
    "description": "Luxury 5-star hotel with premium amenities"
}'
make_request "POST" "$BASE_URL/hotels" "$HOTEL_DATA" "$AUTH_ADMIN"

sleep_short

# Получаем ID созданного отеля
echo "🔹 Получение списка отелей для получения ID"
response=$(curl -s -u $AUTH_USER "$BASE_URL/hotels")
hotel_id=$(echo $response | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
echo "📌 Hotel ID: $hotel_id"

sleep_short

echo "🔹 Получение конкретного отеля (USER)"
make_request "GET" "$BASE_URL/hotels/$hotel_id" "" "$AUTH_USER"

sleep_short

echo "🔹 Обновление отеля (ADMIN)"
UPDATE_HOTEL_DATA='{
    "name": "Grand Plaza Hotel UPDATED",
    "address": "123 Main Street, Updated City Center",
    "description": "Luxury 5-star hotel with premium amenities - UPDATED"
}'
make_request "PUT" "$BASE_URL/hotels/$hotel_id" "$UPDATE_HOTEL_DATA" "$AUTH_ADMIN"

# 2. Тестирование номеров (USER/ADMIN/INTERNAL)
echo ""
echo "2. TESTING ROOMS ENDPOINTS"

sleep_short

echo "🔹 Создание номера (ADMIN)"
ROOM_DATA='{
    "number": "101",
    "type": "DELUXE",
    "price": 299.99,
    "description": "Spacious deluxe room with city view",
    "available": true,
    "timesBooked": 0,
    "hotelId": '$hotel_id'
}'
make_request "POST" "$BASE_URL/rooms" "$ROOM_DATA" "$AUTH_ADMIN"

sleep_short

# Получаем ID созданного номера
echo "🔹 Получение списка номеров для получения ID"
response=$(curl -s -u $AUTH_USER "$BASE_URL/rooms")
room_id=$(echo $response | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
echo "📌 Room ID: $room_id"

sleep_short

echo "🔹 Получение всех доступных номеров (USER)"
make_request "GET" "$BASE_URL/rooms" "" "$AUTH_USER"

sleep_short

echo "🔹 Получение конкретного номера (USER)"
make_request "GET" "$BASE_URL/rooms/$room_id" "" "$AUTH_USER"

sleep_short

echo "🔹 Получение рекомендованных номеров (USER)"
make_request "GET" "$BASE_URL/rooms/recommend" "" "$AUTH_USER"

sleep_short

echo "🔹 Получение номеров по отелю (USER)"
make_request "GET" "$BASE_URL/rooms/hotel/$hotel_id" "" "$AUTH_USER"

sleep_short

echo "🔹 Подтверждение доступности номера (INTERNAL)"
make_request "POST" "$BASE_URL/rooms/$room_id/confirm-availability" "" "$AUTH_INTERNAL"

sleep_short

echo "🔹 Снятие блокировки номера (INTERNAL)"
make_request "POST" "$BASE_URL/rooms/$room_id/release" "" "$AUTH_INTERNAL"

# 3. Тестирование ошибок доступа
echo ""
echo "3. TESTING ACCESS ERRORS"

echo "🔹 Попытка создания отеля без прав ADMIN"
make_request "POST" "$BASE_URL/hotels" "$HOTEL_DATA" "$AUTH_USER"

echo "🔹 Попытка внутреннего endpoint без прав INTERNAL"
make_request "POST" "$BASE_URL/rooms/$room_id/confirm-availability" "" "$AUTH_USER"

echo "🔹 Запрос без аутентификации"
curl -s -w " | HTTP_STATUS:%{http_code}" -X GET "$BASE_URL/hotels"
echo ""
echo "---"

# 4. Очистка (ADMIN)
echo ""
echo "4. CLEANUP"

sleep_short

echo "🔹 Удаление номера (ADMIN)"
make_request "DELETE" "$BASE_URL/rooms/$room_id" "" "$AUTH_ADMIN"

sleep_short

echo "🔹 Удаление отеля (ADMIN)"
make_request "DELETE" "$BASE_URL/hotels/$hotel_id" "" "$AUTH_ADMIN"

echo ""
echo "=================================================="
echo "Testing completed!"
echo "=================================================="
