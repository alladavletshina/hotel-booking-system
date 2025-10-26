#!/bin/bash

# test-hotel-booking-system-fixed.sh
# Исправленный тестовый скрипт для проверки микросервисной системы бронирования отелей

echo "=== Тестирование системы бронирования отелей (исправленная версия) ==="
echo

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Базовые URL
GATEWAY_URL="http://localhost:8080"
AUTH_URL="$GATEWAY_URL/api/auth"
HOTELS_URL="$GATEWAY_URL/api/hotels"
ROOMS_URL="$GATEWAY_URL/api/rooms"
BOOKINGS_URL="$GATEWAY_URL/api/bookings"

# Переменные для хранения токенов
USER_TOKEN=""
ADMIN_TOKEN=""

# Функции для вывода
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

print_debug() {
    echo -e "${BLUE}🐛 $1${NC}"
}

# Функция для выполнения HTTP запросов с улучшенной обработкой ошибок
http_request() {
    local method=$1
    local url=$2
    local data=$3
    local token=$4

    local curl_cmd="curl -s -w \"|%{http_code}\" -X $method \"$url\" -H \"Content-Type: application/json\""

    if [ ! -z "$token" ]; then
        curl_cmd="$curl_cmd -H \"Authorization: Bearer $token\""
    fi

    if [ ! -z "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    local response
    response=$(eval $curl_cmd 2>/dev/null)

    # Разделяем тело ответа и статус код
    local body=$(echo "$response" | sed 's/|.*$//')
    local status_code=$(echo "$response" | sed 's/^.*|//')

    echo "$body"
    return $status_code
}

# Функция для проверки статуса сервисов
check_services() {
    echo "=== Проверка доступности сервисов ==="

    services=("eureka-server:8761" "auth-service:8081" "api-gateway:8080" "hotel-service:8082" "booking-service:8083")

    for service in "${services[@]}"; do
        IFS=':' read -r name port <<< "$service"
        if curl -s "http://localhost:$port/actuator/health" > /dev/null; then
            print_success "$name доступен на порту $port"
        else
            print_error "$name не доступен на порту $port"
        fi
    done
    echo
}

# 1. Аутентификация пользователей (используем существующих)
authenticate_users() {
    echo "=== 1. Аутентификация пользователей ==="

    # Аутентификация обычного пользователя
    print_info "Аутентификация пользователя testuser..."
    LOGIN_RESPONSE=$(http_request "POST" "$AUTH_URL/login" '{
        "username": "testuser",
        "password": "password123"
    }')

    local status_code=$?
    if [ $status_code -eq 200 ] && echo "$LOGIN_RESPONSE" | grep -q "token"; then
        USER_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        print_success "Пользователь аутентифицирован. Токен получен"
        print_debug "Токен: ${USER_TOKEN:0:50}..."
    else
        print_error "Ошибка аутентификации пользователя. Код: $status_code"
        print_debug "Ответ: $LOGIN_RESPONSE"

        # Попробуем зарегистрировать нового пользователя
        print_info "Регистрация нового пользователя..."
        REGISTER_RESPONSE=$(http_request "POST" "$AUTH_URL/register" '{
            "username": "user2",
            "password": "password123",
            "email": "user2@example.com",
            "firstName": "User",
            "lastName": "Two"
        }')

        if echo "$REGISTER_RESPONSE" | grep -q "token"; then
            USER_TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
            print_success "Новый пользователь зарегистрирован. Токен получен"
        else
            print_error "Ошибка регистрации: $REGISTER_RESPONSE"
        fi
    fi

    # Аутентификация администратора
    print_info "Аутентификация администратора admin..."
    ADMIN_LOGIN_RESPONSE=$(http_request "POST" "$AUTH_URL/login" '{
        "username": "admin",
        "password": "admin123"
    }')

    local admin_status_code=$?
    if [ $admin_status_code -eq 200 ] && echo "$ADMIN_LOGIN_RESPONSE" | grep -q "token"; then
        ADMIN_TOKEN=$(echo "$ADMIN_LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        print_success "Администратор аутентифицирован. Токен получен"
        print_debug "Токен: ${ADMIN_TOKEN:0:50}..."
    else
        print_error "Ошибка аутентификации администратора. Код: $admin_status_code"
        print_debug "Ответ: $ADMIN_LOGIN_RESPONSE"

        # Попробуем зарегистрировать нового администратора
        print_info "Регистрация нового администратора..."
        ADMIN_REGISTER_RESPONSE=$(http_request "POST" "$AUTH_URL/register" '{
            "username": "admin2",
            "password": "admin123",
            "email": "admin2@example.com",
            "firstName": "Admin",
            "lastName": "Two",
            "role": "ADMIN"
        }')

        if echo "$ADMIN_REGISTER_RESPONSE" | grep -q "token"; then
            ADMIN_TOKEN=$(echo "$ADMIN_REGISTER_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
            print_success "Новый администратор зарегистрирован. Токен получен"
        else
            print_error "Ошибка регистрации администратора: $ADMIN_REGISTER_RESPONSE"
        fi
    fi

    # Проверка токена пользователя
    if [ ! -z "$USER_TOKEN" ]; then
        print_info "Проверка токена пользователя..."
        VALIDATE_RESPONSE=$(http_request "GET" "$AUTH_URL/validate" "" "$USER_TOKEN")
        local validate_status=$?
        if [ $validate_status -eq 200 ]; then
            print_success "Токен пользователя валиден"
        else
            print_error "Токен пользователя невалиден. Код: $validate_status"
        fi
    fi
    echo
}

# 2. Работа с отелями (только для ADMIN)
manage_hotels() {
    echo "=== 2. Управление отелями (ADMIN) ==="

    if [ -z "$ADMIN_TOKEN" ]; then
        print_error "Токен администратора отсутствует, пропускаем тест отелей"
        return 1
    fi

    # Создание отеля
    print_info "Создание отеля..."
    HOTEL_RESPONSE=$(http_request "POST" "$HOTELS_URL" '{
        "name": "Grand Plaza Hotel",
        "address": "123 Main Street, Moscow",
        "description": "Luxury 5-star hotel in city center"
    }' "$ADMIN_TOKEN")

    local hotel_status=$?
    if [ $hotel_status -eq 200 ] && echo "$HOTEL_RESPONSE" | grep -q "id"; then
        HOTEL_ID=$(echo "$HOTEL_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
        print_success "Отель создан. ID: $HOTEL_ID"
    else
        print_error "Ошибка создания отеля. Код: $hotel_status"
        print_debug "Ответ: $HOTEL_RESPONSE"

        # Попробуем получить существующие отели
        print_info "Получение существующих отелей..."
        EXISTING_HOTELS=$(http_request "GET" "$HOTELS_URL" "" "$ADMIN_TOKEN")
        if echo "$EXISTING_HOTELS" | grep -q "id"; then
            HOTEL_ID=$(echo "$EXISTING_HOTELS" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
            print_success "Найден существующий отель. ID: $HOTEL_ID"
        else
            print_error "Не удалось найти или создать отель"
            return 1
        fi
    fi

    # Создание номера
    print_info "Создание номера в отеле..."
    ROOM_RESPONSE=$(http_request "POST" "$ROOMS_URL" '{
        "number": "101",
        "type": "DELUXE",
        "price": 5000.0,
        "available": true,
        "hotelId": '$HOTEL_ID'
    }' "$ADMIN_TOKEN")

    local room_status=$?
    if [ $room_status -eq 200 ] && echo "$ROOM_RESPONSE" | grep -q "id"; then
        ROOM_ID=$(echo "$ROOM_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
        print_success "Номер создан. ID: $ROOM_ID"
    else
        print_error "Ошибка создания номера. Код: $room_status"
        print_debug "Ответ: $ROOM_RESPONSE"

        # Попробуем получить существующие номера
        print_info "Получение существующих номеров..."
        EXISTING_ROOMS=$(http_request "GET" "$ROOMS_URL" "" "$ADMIN_TOKEN")
        if echo "$EXISTING_ROOMS" | grep -q "id"; then
            ROOM_ID=$(echo "$EXISTING_ROOMS" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
            print_success "Найден существующий номер. ID: $ROOM_ID"
        else
            print_error "Не удалось найти или создать номер"
            return 1
        fi
    fi

    # Получение списка отелей (для USER)
    if [ ! -z "$USER_TOKEN" ]; then
        print_info "Получение списка отелей пользователем..."
        HOTELS_LIST_RESPONSE=$(http_request "GET" "$HOTELS_URL" "" "$USER_TOKEN")
        local list_status=$?
        if [ $list_status -eq 200 ]; then
            print_success "Список отелей получен успешно"
        else
            print_error "Ошибка получения списка отелей. Код: $list_status"
        fi
    fi
    echo
}

# 3. Работа с номерами
manage_rooms() {
    echo "=== 3. Работа с номерами ==="

    if [ -z "$USER_TOKEN" ]; then
        print_error "Токен пользователя отсутствует, пропускаем тест номеров"
        return 1
    fi

    # Получение доступных номеров
    print_info "Получение доступных номеров..."
    AVAILABLE_ROOMS_RESPONSE=$(http_request "GET" "$ROOMS_URL" "" "$USER_TOKEN")
    local available_status=$?
    if [ $available_status -eq 200 ]; then
        if echo "$AVAILABLE_ROOMS_RESPONSE" | grep -q "id"; then
            print_success "Список доступных номеров получен"
            local room_count=$(echo "$AVAILABLE_ROOMS_RESPONSE" | grep -o '"id"' | wc -l)
            print_info "Найдено номеров: $room_count"
        else
            print_info "Доступные номера не найдены"
        fi
    else
        print_error "Ошибка получения доступных номеров. Код: $available_status"
    fi

    # Получение рекомендованных номеров
    print_info "Получение рекомендованных номеров..."
    RECOMMENDED_ROOMS_RESPONSE=$(http_request "GET" "$ROOMS_URL/recommend" "" "$USER_TOKEN")
    local recommended_status=$?
    if [ $recommended_status -eq 200 ]; then
        if echo "$RECOMMENDED_ROOMS_RESPONSE" | grep -q "id"; then
            print_success "Список рекомендованных номеров получен"
        else
            print_info "Рекомендованные номера не найдены"
        fi
    else
        print_error "Ошибка получения рекомендованных номеров. Код: $recommended_status"
    fi
    echo
}

# 4. Бронирования
manage_bookings() {
    echo "=== 4. Управление бронированиями ==="

    if [ -z "$USER_TOKEN" ]; then
        print_error "Токен пользователя отсутствует, пропускаем тест бронирований"
        return 1
    fi

    if [ -z "$ROOM_ID" ]; then
        print_error "ID номера не определен, пропускаем тест бронирований"
        return 1
    fi

    # Используем даты в будущем
    local start_date=$(date -v+2d +%Y-%m-%d 2>/dev/null || date -d "+2 days" +%Y-%m-%d)
    local end_date=$(date -v+5d +%Y-%m-%d 2>/dev/null || date -d "+5 days" +%Y-%m-%d)

    # Создание бронирования
    print_info "Создание бронирования (номера $ROOM_ID, даты: $start_date - $end_date)..."
    BOOKING_RESPONSE=$(http_request "POST" "$BOOKINGS_URL" '{
        "roomId": '$ROOM_ID',
        "startDate": "'$start_date'",
        "endDate": "'$end_date'",
        "correlationId": "test-booking-'$(date +%s)'"
    }' "$USER_TOKEN")

    local booking_status=$?
    if [ $booking_status -eq 200 ] && echo "$BOOKING_RESPONSE" | grep -q "id"; then
        BOOKING_ID=$(echo "$BOOKING_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
        BOOKING_STATUS=$(echo "$BOOKING_RESPONSE" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
        print_success "Бронирование создано. ID: $BOOKING_ID, Статус: $BOOKING_STATUS"
    else
        print_error "Ошибка создания бронирования. Код: $booking_status"
        print_debug "Ответ: $BOOKING_RESPONSE"
        return 1
    fi

    # Получение бронирований пользователя
    print_info "Получение бронирований пользователя..."
    USER_BOOKINGS_RESPONSE=$(http_request "GET" "$BOOKINGS_URL/my" "" "$USER_TOKEN")
    local user_bookings_status=$?
    if [ $user_bookings_status -eq 200 ]; then
        if echo "$USER_BOOKINGS_RESPONSE" | grep -q "id"; then
            print_success "Список бронирований пользователя получен"
            local booking_count=$(echo "$USER_BOOKINGS_RESPONSE" | grep -o '"id"' | wc -l)
            print_info "Найдено бронирований: $booking_count"
        else
            print_info "Бронирования не найдены"
        fi
    else
        print_error "Ошибка получения бронирований. Код: $user_bookings_status"
    fi

    # Отмена бронирования
    print_info "Отмена бронирования $BOOKING_ID..."
    CANCEL_RESPONSE=$(http_request "DELETE" "$BOOKINGS_URL/$BOOKING_ID" "" "$USER_TOKEN")
    local cancel_status=$?
    if [ $cancel_status -eq 200 ]; then
        print_success "Бронирование отменено"
    else
        print_error "Ошибка отмены бронирования. Код: $cancel_status"
        print_debug "Ответ: $CANCEL_RESPONSE"
    fi
    echo
}

# 5. Тестирование ошибок и граничных случаев
test_error_cases() {
    echo "=== 5. Тестирование ошибок и граничных случаев ==="

    # Попытка создания отеля без прав ADMIN
    if [ ! -z "$USER_TOKEN" ]; then
        print_info "Попытка создания отеля пользователем (должна быть ошибка)..."
        UNAUTHORIZED_HOTEL_RESPONSE=$(http_request "POST" "$HOTELS_URL" '{
            "name": "Unauthorized Hotel",
            "address": "Test Address"
        }' "$USER_TOKEN")

        local unauthorized_status=$?
        if [ $unauthorized_status -eq 403 ] || [ $unauthorized_status -eq 401 ]; then
            print_success "Доступ запрещен (как и ожидалось). Код: $unauthorized_status"
        elif echo "$UNAUTHORIZED_HOTEL_RESPONSE" | grep -q "error\|denied\|access"; then
            print_success "Доступ запрещен (как и ожидалось)"
        else
            print_error "Неожиданный ответ при создании отеля пользователем. Код: $unauthorized_status"
            print_debug "Ответ: $UNAUTHORIZED_HOTEL_RESPONSE"
        fi
    fi

    # Попытка бронирования с невалидными датами
    if [ ! -z "$USER_TOKEN" ] && [ ! -z "$ROOM_ID" ]; then
        print_info "Попытка бронирования с невалидными датами..."
        INVALID_DATE_RESPONSE=$(http_request "POST" "$BOOKINGS_URL" '{
            "roomId": '$ROOM_ID',
            "startDate": "2023-01-01",
            "endDate": "2023-01-05"
        }' "$USER_TOKEN")

        local invalid_date_status=$?
        if [ $invalid_date_status -eq 400 ]; then
            print_success "Валидация дат работает (как и ожидалось). Код: $invalid_date_status"
        elif echo "$INVALID_DATE_RESPONSE" | grep -q "error\|past\|invalid"; then
            print_success "Валидация дат работает (как и ожидалось)"
        else
            print_error "Неожиданный ответ при невалидных датах. Код: $invalid_date_status"
            print_debug "Ответ: $INVALID_DATE_RESPONSE"
        fi
    fi

    # Попытка доступа без токена
    print_info "Попытка доступа к защищенному эндпоинту без токена..."
    NO_TOKEN_RESPONSE=$(http_request "GET" "$HOTELS_URL" "")
    local no_token_status=$?
    if [ $no_token_status -eq 401 ] || [ $no_token_status -eq 403 ]; then
        print_success "Защита от неавторизованного доступа работает. Код: $no_token_status"
    else
        print_error "Неожиданный ответ без токена. Код: $no_token_status"
        print_debug "Ответ: $NO_TOKEN_RESPONSE"
    fi
    echo
}

# 6. Проверка Eureka
check_eureka() {
    echo "=== 6. Проверка регистрации в Eureka ==="

    EUREKA_RESPONSE=$(curl -s "http://localhost:8761" | grep -o "hotel-booking-system\|Eureka" | head -1)
    if [ ! -z "$EUREKA_RESPONSE" ]; then
        print_success "Eureka доступен"

        # Проверяем регистрацию сервисов
        SERVICES_REGISTERED=$(curl -s "http://localhost:8761" | grep -c "DS Replicas")
        if [ $SERVICES_REGISTERED -gt 0 ]; then
            print_success "Сервисы зарегистрированы в Eureka"
        else
            print_info "Eureka работает, но сервисы могут быть не зарегистрированы"
        fi
    else
        print_error "Eureka не доступен или не отвечает"
    fi
    echo
}

# Главная функция
main() {
    echo "Начало тестирования системы бронирования отелей"
    echo "=============================================="
    echo

    # Проверка доступности сервисов
    check_services

    # Аутентификация
    authenticate_users

    # Управление отелями и номерами
    manage_hotels
    manage_rooms

    # Бронирования
    manage_bookings

    # Тестирование ошибок
    test_error_cases

    # Проверка Eureka
    check_eureka

    echo "=============================================="
    echo "Тестирование завершено"
    echo

    if [ ! -z "$USER_TOKEN" ]; then
        echo "Токен пользователя: ${USER_TOKEN:0:50}..."
    fi
    if [ ! -z "$ADMIN_TOKEN" ]; then
        echo "Токен администратора: ${ADMIN_TOKEN:0:50}..."
    fi
    if [ ! -z "$ROOM_ID" ]; then
        echo "ID номера для тестирования: $ROOM_ID"
    fi

    echo
    echo "Для дополнительного тестирования используйте:"
    echo "  Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "  Eureka: http://localhost:8761"
    echo "  H2 Console Hotel: http://localhost:8082/h2-console"
    echo "  H2 Console Booking: http://localhost:8083/h2-console"
}

# Запуск тестов
main