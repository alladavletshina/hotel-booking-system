#!/bin/bash

# test-rooms.sh
# Тестирование Room Management API

BASE_URL="http://localhost:8080/api"
AUTH_USER="user:password"
AUTH_ADMIN="admin:password"
AUTH_INTERNAL="internal:password"

echo "=================================================="
echo "Room Management API Tests"
echo "Started at: $(date)"
echo "=================================================="

# Функция для выполнения запросов с логированием
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local auth=$4
    local description=$5

    echo ""
    echo "🔹 $description"
    echo "➡️ $method $url"

    local curl_cmd="curl -s -w ' | HTTP_STATUS:%{http_code}' -X $method '$url' -H 'Content-Type: application/json' -H 'Accept: application/json'"

    if [ ! -z "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    if [ ! -z "$auth" ]; then
        curl_cmd="$curl_cmd -u $auth"
    fi

    local response=$(eval $curl_cmd)
    echo "$response"

    # Извлекаем HTTP статус
    local http_status=$(echo "$response" | grep -o 'HTTP_STATUS:[0-9]*' | cut -d':' -f2)

    if [ "$http_status" -eq 200 ] || [ "$http_status" -eq 201 ]; then
        echo "✅ SUCCESS (HTTP $http_status)"
    else
        echo "❌ FAILED (HTTP $http_status)"
    fi

    sleep 1
}

# Функция для извлечения ID из JSON ответа
extract_id() {
    echo "$1" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1
}

# Функция для получения ID отеля
get_hotel_id() {
    local hotels_response=$(curl -s -u $AUTH_USER "$BASE_URL/hotels")
    extract_id "$hotels_response"
}

echo ""
echo "1. 🏨 ПОДГОТОВКА: ПОЛУЧЕНИЕ ID ОТЕЛЯ"

hotel_id=$(get_hotel_id)

if [ -z "$hotel_id" ]; then
    echo "⚠️ Отели не найдены, создаем тестовый отель..."

    HOTEL_DATA='{
        "name": "Test Hotel for Rooms",
        "address": "123 Test Street",
        "description": "Test hotel for room testing"
    }'

    hotel_response=$(curl -s -u $AUTH_ADMIN -X POST "$BASE_URL/hotels" \
        -H "Content-Type: application/json" \
        -d "$HOTEL_DATA")

    hotel_id=$(extract_id "$hotel_response")
    echo "✅ Создан отель с ID: $hotel_id"
else
    echo "✅ Найден отель с ID: $hotel_id"
fi

echo ""
echo "2. 🔍 ПОЛУЧЕНИЕ ДАННЫХ (USER ROLE)"

make_request "GET" "$BASE_URL/rooms" "" "$AUTH_USER" "Получить все доступные номера"

make_request "GET" "$BASE_URL/rooms/recommend" "" "$AUTH_USER" "Получить рекомендованные номера"

make_request "GET" "$BASE_URL/rooms/hotel/$hotel_id" "" "$AUTH_USER" "Получить номера по отелю $hotel_id"

echo ""
echo "3. 🛏️ СОЗДАНИЕ НОМЕРОВ (ADMIN ROLE)"

ROOM_1_DATA='{
    "number": "101",
    "type": "DELUXE",
    "price": 199.99,
    "description": "Spacious deluxe room with city view",
    "available": true,
    "timesBooked": 5,
    "hotelId": '$hotel_id'
}'

make_request "POST" "$BASE_URL/rooms" "$ROOM_1_DATA" "$AUTH_ADMIN" "Создать делюкс номер 101"

ROOM_2_DATA='{
    "number": "102",
    "type": "SUITE",
    "price": 299.99,
    "description": "Luxury suite with jacuzzi and balcony",
    "available": true,
    "timesBooked": 2,
    "hotelId": '$hotel_id'
}'

make_request "POST" "$BASE_URL/rooms" "$ROOM_2_DATA" "$AUTH_ADMIN" "Создать люкс номер 102"

ROOM_3_DATA='{
    "number": "103",
    "type": "SINGLE",
    "price": 99.99,
    "description": "Cozy single room",
    "available": false,
    "timesBooked": 10,
    "hotelId": '$hotel_id'
}'

make_request "POST" "$BASE_URL/rooms" "$ROOM_3_DATA" "$AUTH_ADMIN" "Создать недоступный номер 103"

echo ""
echo "4. 🔍 ПРОВЕРКА СОЗДАННЫХ НОМЕРОВ"

# Получаем ID созданных номеров
rooms_response=$(curl -s -u $AUTH_USER "$BASE_URL/rooms")
room_id=$(extract_id "$rooms_response")

if [ ! -z "$room_id" ]; then
    make_request "GET" "$BASE_URL/rooms/$room_id" "" "$AUTH_USER" "Получить номер по ID $room_id"

    echo ""
    echo "📊 Сравнение endpoints:"
    echo ""

    echo "Все доступные номера:"
    curl -s -u $AUTH_USER "$BASE_URL/rooms" | grep -o '"id":[0-9]*' | sort
    echo ""

    echo "Рекомендованные номера (по timesBooked):"
    curl -s -u $AUTH_USER "$BASE_URL/rooms/recommend" | grep -o '"id":[0-9]*' | sort
    echo ""

    echo "Номера отеля $hotel_id:"
    curl -s -u $AUTH_USER "$BASE_URL/rooms/hotel/$hotel_id" | grep -o '"id":[0-9]*' | sort
else
    echo "❌ Не удалось получить ID номера"
fi

echo ""
echo "5. 🔒 INTERNAL ENDPOINTS (INTERNAL ROLE)"

if [ ! -z "$room_id" ]; then
    make_request "POST" "$BASE_URL/rooms/$room_id/confirm-availability" "" "$AUTH_INTERNAL" "Подтвердить доступность номера $room_id"

    make_request "POST" "$BASE_URL/rooms/$room_id/release" "" "$AUTH_INTERNAL" "Снять блокировку номера $room_id"
else
    echo "❌ Не удалось получить ID номера для тестирования internal endpoints"
fi

echo ""
echo "6. 🚫 ТЕСТИРОВАНИЕ ДОСТУПА"

# Попытка создать номер с USER ролью (должно быть 403)
make_request "POST" "$BASE_URL/rooms" "$ROOM_1_DATA" "$AUTH_USER" "Попытка создать номер с USER ролью (ожидается 403)"

# Попытка использовать internal endpoint с USER ролью (должно быть 403)
if [ ! -z "$room_id" ]; then
    make_request "POST" "$BASE_URL/rooms/$room_id/confirm-availability" "" "$AUTH_USER" "Попытка подтвердить доступность с USER ролью (ожидается 403)"
fi

# Запрос без аутентификации (должно быть 401)
echo ""
echo "🔹 Запрос без аутентификации"
curl -s -w " | HTTP_STATUS:%{http_code}" -X GET "$BASE_URL/rooms"
echo ""

echo ""
echo "7. 🗑️ ОЧИСТКА (ADMIN ROLE)"

if [ ! -z "$room_id" ]; then
    # Удаляем созданные номера
    rooms_response=$(curl -s -u $AUTH_USER "$BASE_URL/rooms")
    room_ids=$(echo "$rooms_response" | grep -o '"id":[0-9]*' | cut -d':' -f2)

    for id in $room_ids; do
        make_request "DELETE" "$BASE_URL/rooms/$id" "" "$AUTH_ADMIN" "Удалить номер $id"
    done

    # Удаляем тестовый отель если создавали
    if [ ! -z "$hotel_id" ]; then
        make_request "DELETE" "$BASE_URL/hotels/$hotel_id" "" "$AUTH_ADMIN" "Удалить тестовый отель $hotel_id"
    fi
fi

echo ""
echo "=================================================="
echo "🏁 ТЕСТИРОВАНИЕ ЗАВЕРШЕНО"
echo "Finished at: $(date)"
echo "=================================================="