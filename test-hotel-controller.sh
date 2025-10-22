#!/bin/bash

# test-hotels.sh
# Тестирование Hotel Management API

BASE_URL="http://localhost:8080/api"
AUTH_USER="user:password"
AUTH_ADMIN="admin:password"

echo "=================================================="
echo "Hotel Management API Tests"
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

echo ""
echo "1. 🔍 ПОЛУЧЕНИЕ ДАННЫХ (USER ROLE)"

make_request "GET" "$BASE_URL/hotels" "" "$AUTH_USER" "Получить все отели"

# Сохраняем список отелей для получения ID
hotels_response=$(curl -s -u $AUTH_USER "$BASE_URL/hotels")
first_hotel_id=$(extract_id "$hotels_response")

if [ ! -z "$first_hotel_id" ]; then
    make_request "GET" "$BASE_URL/hotels/$first_hotel_id" "" "$AUTH_USER" "Получить отель по ID ($first_hotel_id)"
else
    echo ""
    echo "⚠️ Нет отелей для тестирования, создаем новый..."
fi

echo ""
echo "2. 🏨 СОЗДАНИЕ ОТЕЛЕЙ (ADMIN ROLE)"

HOTEL_1_DATA='{
    "name": "Grand Plaza Hotel",
    "address": "123 Main Street, City Center",
    "description": "Luxury 5-star hotel with premium amenities"
}'

make_request "POST" "$BASE_URL/hotels" "$HOTEL_1_DATA" "$AUTH_ADMIN" "Создать отель Grand Plaza"

HOTEL_2_DATA='{
    "name": "Seaside Resort",
    "address": "456 Beach Boulevard, Ocean View",
    "description": "Beautiful resort with ocean views and private beach"
}'

make_request "POST" "$BASE_URL/hotels" "$HOTEL_2_DATA" "$AUTH_ADMIN" "Создать отель Seaside Resort"

echo ""
echo "3. 🔄 ОБНОВЛЕНИЕ ОТЕЛЕЙ (ADMIN ROLE)"

# Получаем ID последнего созданного отеля
hotels_response=$(curl -s -u $AUTH_USER "$BASE_URL/hotels")
hotel_id=$(extract_id "$hotels_response")

if [ ! -z "$hotel_id" ]; then
    UPDATE_HOTEL_DATA='{
        "name": "Grand Plaza Hotel UPDATED",
        "address": "123 Updated Street, New City Center",
        "description": "Luxury 5-star hotel - RECENTLY RENOVATED"
    }'

    make_request "PUT" "$BASE_URL/hotels/$hotel_id" "$UPDATE_HOTEL_DATA" "$AUTH_ADMIN" "Обновить отель $hotel_id"

    echo ""
    echo "4. 🧪 ПРОВЕРКА ОБНОВЛЕННЫХ ДАННЫХ"
    make_request "GET" "$BASE_URL/hotels/$hotel_id" "" "$AUTH_USER" "Проверить обновленный отель"
else
    echo "❌ Не удалось получить ID отеля для обновления"
fi

echo ""
echo "5. 🚫 ТЕСТИРОВАНИЕ ДОСТУПА"

# Попытка создать отель с USER ролью (должно быть 403)
make_request "POST" "$BASE_URL/hotels" "$HOTEL_1_DATA" "$AUTH_USER" "Попытка создать отель с USER ролью (ожидается 403)"

# Запрос без аутентификации (должно быть 401)
echo ""
echo "🔹 Запрос без аутентификации"
curl -s -w " | HTTP_STATUS:%{http_code}" -X GET "$BASE_URL/hotels"
echo ""

echo ""
echo "6. 🗑️ ОЧИСТКА (ADMIN ROLE)"

if [ ! -z "$hotel_id" ]; then
    make_request "DELETE" "$BASE_URL/hotels/$hotel_id" "" "$AUTH_ADMIN" "Удалить отель $hotel_id"

    # Проверяем что отель удален
    echo ""
    echo "🔹 Проверка удаления отеля"
    curl -s -u $AUTH_USER -w " | HTTP_STATUS:%{http_code}" -X GET "$BASE_URL/hotels/$hotel_id"
    echo ""
fi

echo ""
echo "=================================================="
echo "🏁 ТЕСТИРОВАНИЕ ЗАВЕРШЕНО"
echo "Finished at: $(date)"
echo "=================================================="