#!/bin/bash

# test-hotel-booking-system-complete.sh
# Полное тестирование всех контроллеров микросервисной системы бронирования отелей

echo "=== ПОЛНОЕ ТЕСТИРОВАНИЕ СИСТЕМЫ БРОНИРОВАНИЯ ОТЕЛЕЙ ==="
echo "=== Проверка всех контроллеров и endpoints ==="
echo

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Базовые URL
GATEWAY_URL="http://localhost:8080"
AUTH_URL="$GATEWAY_URL/api/auth"
HOTELS_URL="$GATEWAY_URL/api/hotels"
ROOMS_URL="$GATEWAY_URL/api/rooms"
BOOKINGS_URL="$GATEWAY_URL/api/bookings"
USERS_URL="$GATEWAY_URL/api/admin/users"

# Переменные для хранения данных
USER_TOKEN=""
ADMIN_TOKEN=""
HOTEL_ID=""
ROOM_ID=""
BOOKING_ID=""
USER_ID=""

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

print_section() {
    echo -e "${PURPLE}=== $1 ===${NC}"
}

print_endpoint() {
    echo -e "${CYAN}➤ $1${NC}"
}

# Функция для выполнения HTTP запросов
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

    local body=$(echo "$response" | sed 's/|.*$//')
    local status_code=$(echo "$response" | sed 's/^.*|//')

    echo "$body"
    return $status_code
}

# 1. Проверка здоровья сервисов
check_services_health() {
    print_section "1. ПРОВЕРКА ЗДОРОВЬЯ СЕРВИСОВ"

    services=(
        "eureka-server:8761"
        "auth-service:8081"
        "api-gateway:8080"
        "hotel-service:8082"
        "booking-service:8083"
    )

    for service in "${services[@]}"; do
        IFS=':' read -r name port <<< "$service"
        print_endpoint "Проверка $name"
        if curl -s "http://localhost:$port/actuator/health" > /dev/null; then
            print_success "$name доступен на порту $port"
        else
            print_error "$name не доступен на порту $port"
        fi
    done
    echo
}

# 2. Аутентификация и регистрация
test_auth_controller() {
    print_section "2. ТЕСТИРОВАНИЕ AUTH CONTROLLER"

    # Регистрация нового пользователя
    print_endpoint "POST /api/auth/register - Регистрация пользователя"
    REGISTER_RESPONSE=$(http_request "POST" "$AUTH_URL/register" '{
        "username": "testuser",
        "password": "password123",
        "email": "testuser@example.com",
        "firstName": "Test",
        "lastName": "User"
    }')

    local register_status=$?
    if [ $register_status -eq 200 ] && echo "$REGISTER_RESPONSE" | grep -q "token"; then
        USER_TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        print_success "Пользователь зарегистрирован. Токен получен"
    else
        print_error "Ошибка регистрации. Код: $register_status"
        print_debug "Ответ: $REGISTER_RESPONSE"
    fi

    # Аутентификация администратора
    print_endpoint "POST /api/auth/login - Аутентификация администратора"
    ADMIN_LOGIN_RESPONSE=$(http_request "POST" "$AUTH_URL/login" '{
        "username": "admin",
        "password": "admin123"
    }')

    local admin_status=$?
    if [ $admin_status -eq 200 ] && echo "$ADMIN_LOGIN_RESPONSE" | grep -q "token"; then
        ADMIN_TOKEN=$(echo "$ADMIN_LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        print_success "Администратор аутентифицирован. Токен получен"
    else
        print_error "Ошибка аутентификации администратора. Код: $admin_status"
    fi

    # Проверка валидации токена
    if [ ! -z "$USER_TOKEN" ]; then
        print_endpoint "GET /api/auth/validate - Проверка токена"
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

# 3. Тестирование Hotel Controller
test_hotel_controller() {
    print_section "3. ТЕСТИРОВАНИЕ HOTEL CONTROLLER"

    if [ -z "$ADMIN_TOKEN" ]; then
        print_error "Токен администратора отсутствует, пропускаем тест отелей"
        return 1
    fi

    # Создание отеля
    print_endpoint "POST /api/hotels - Создание отеля (ADMIN)"
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
        return 1
    fi

    # Получение всех отелей
    print_endpoint "GET /api/hotels - Получение всех отелей"
    HOTELS_LIST_RESPONSE=$(http_request "GET" "$HOTELS_URL" "" "$USER_TOKEN")
    local list_status=$?
    if [ $list_status -eq 200 ]; then
        print_success "Список отелей получен успешно"
    else
        print_error "Ошибка получения списка отелей. Код: $list_status"
    fi

    # Получение отеля по ID
    print_endpoint "GET /api/hotels/{id} - Получение отеля по ID"
    HOTEL_BY_ID_RESPONSE=$(http_request "GET" "$HOTELS_URL/$HOTEL_ID" "" "$USER_TOKEN")
    local hotel_by_id_status=$?
    if [ $hotel_by_id_status -eq 200 ]; then
        print_success "Отель по ID получен успешно"
    else
        print_error "Ошибка получения отеля по ID. Код: $hotel_by_id_status"
    fi

    # Обновление отеля
    print_endpoint "PUT /api/hotels/{id} - Обновление отеля (ADMIN)"
    UPDATE_HOTEL_RESPONSE=$(http_request "PUT" "$HOTELS_URL/$HOTEL_ID" '{
        "name": "Grand Plaza Hotel Updated",
        "address": "123 Main Street, Moscow, Updated",
        "description": "Luxury 5-star hotel - recently renovated"
    }' "$ADMIN_TOKEN")

    local update_status=$?
    if [ $update_status -eq 200 ]; then
        print_success "Отель обновлен успешно"
    else
        print_error "Ошибка обновления отеля. Код: $update_status"
    fi
    echo
}

# 4. Тестирование Room Controller
test_room_controller() {
    print_section "4. ТЕСТИРОВАНИЕ ROOM CONTROLLER"

    if [ -z "$ADMIN_TOKEN" ] || [ -z "$HOTEL_ID" ]; then
        print_error "Токен администратора или ID отеля отсутствует"
        return 1
    fi

    # Создание номера
    print_endpoint "POST /api/rooms - Создание номера (ADMIN)"
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
        return 1
    fi

    # Получение всех номеров
    print_endpoint "GET /api/rooms - Получение доступных номеров"
    ROOMS_RESPONSE=$(http_request "GET" "$ROOMS_URL" "" "$USER_TOKEN")
    local rooms_status=$?
    if [ $rooms_status -eq 200 ]; then
        print_success "Список номеров получен успешно"
    else
        print_error "Ошибка получения списка номеров. Код: $rooms_status"
    fi

    # Получение номера по ID
    print_endpoint "GET /api/rooms/{id} - Получение номера по ID"
    ROOM_BY_ID_RESPONSE=$(http_request "GET" "$ROOMS_URL/$ROOM_ID" "" "$USER_TOKEN")
    local room_by_id_status=$?
    if [ $room_by_id_status -eq 200 ]; then
        print_success "Номер по ID получен успешно"
    else
        print_error "Ошибка получения номера по ID. Код: $room_by_id_status"
    fi

    # Получение номеров по отелю
    print_endpoint "GET /api/rooms/hotel/{hotelId} - Получение номеров отеля"
    ROOMS_BY_HOTEL_RESPONSE=$(http_request "GET" "$ROOMS_URL/hotel/$HOTEL_ID" "" "$USER_TOKEN")
    local rooms_by_hotel_status=$?
    if [ $rooms_by_hotel_status -eq 200 ]; then
        print_success "Номера отеля получены успешно"
    else
        print_error "Ошибка получения номеров отеля. Код: $rooms_by_hotel_status"
    fi

    # Получение рекомендованных номеров
    print_endpoint "GET /api/rooms/recommend - Получение рекомендованных номеров"
    RECOMMENDED_RESPONSE=$(http_request "GET" "$ROOMS_URL/recommend" "" "$USER_TOKEN")
    local recommended_status=$?
    if [ $recommended_status -eq 200 ]; then
        print_success "Рекомендованные номера получены успешно"
    else
        print_error "Ошибка получения рекомендованных номеров. Код: $recommended_status"
    fi

    # Обновление номера
    print_endpoint "PUT /api/rooms/{id} - Обновление номера (ADMIN)"
    UPDATE_ROOM_RESPONSE=$(http_request "PUT" "$ROOMS_URL/$ROOM_ID" '{
        "number": "101",
        "type": "SUPER_DELUXE",
        "price": 7500.0,
        "available": true,
        "hotelId": '$HOTEL_ID'
    }' "$ADMIN_TOKEN")

    local update_room_status=$?
    if [ $update_room_status -eq 200 ]; then
        print_success "Номер обновлен успешно"
    else
        print_error "Ошибка обновления номера. Код: $update_room_status"
    fi
    echo
}

# 5. Тестирование Booking Controller
test_booking_controller() {
    print_section "5. ТЕСТИРОВАНИЕ BOOKING CONTROLLER"

    if [ -z "$USER_TOKEN" ] || [ -z "$ROOM_ID" ]; then
        print_error "Токен пользователя или ID номера отсутствует"
        return 1
    fi

    # Создание бронирования
    local start_date=$(date -v+2d +%Y-%m-%d 2>/dev/null || date -d "+2 days" +%Y-%m-%d)
    local end_date=$(date -v+5d +%Y-%m-%d 2>/dev/null || date -d "+5 days" +%Y-%m-%d)

    print_endpoint "POST /api/bookings - Создание бронирования"
    BOOKING_RESPONSE=$(http_request "POST" "$BOOKINGS_URL" '{
        "roomId": '$ROOM_ID',
        "startDate": "'$start_date'",
        "endDate": "'$end_date'",
        "correlationId": "complete-test-'$(date +%s)'"
    }' "$USER_TOKEN")

    local booking_status=$?
    if [ $booking_status -eq 200 ] && echo "$BOOKING_RESPONSE" | grep -q "id"; then
        BOOKING_ID=$(echo "$BOOKING_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
        BOOKING_STATUS=$(echo "$BOOKING_RESPONSE" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
        print_success "Бронирование создано. ID: $BOOKING_ID, Статус: $BOOKING_STATUS"
    else
        print_error "Ошибка создания бронирования. Код: $booking_status"
        return 1
    fi

    # Получение моих бронирований
    print_endpoint "GET /api/bookings/my - Получение моих бронирований"
    MY_BOOKINGS_RESPONSE=$(http_request "GET" "$BOOKINGS_URL/my" "" "$USER_TOKEN")
    local my_bookings_status=$?
    if [ $my_bookings_status -eq 200 ]; then
        print_success "Мои бронирования получены успешно"
    else
        print_error "Ошибка получения моих бронирований. Код: $my_bookings_status"
    fi

    # Получение всех бронирований (ADMIN)
    print_endpoint "GET /api/bookings - Получение всех бронирований (ADMIN)"
    ALL_BOOKINGS_RESPONSE=$(http_request "GET" "$BOOKINGS_URL" "" "$ADMIN_TOKEN")
    local all_bookings_status=$?
    if [ $all_bookings_status -eq 200 ]; then
        print_success "Все бронирования получены успешно (ADMIN)"
    else
        print_error "Ошибка получения всех бронирований. Код: $all_bookings_status"
    fi

    # Получение бронирований пользователя по ID (ADMIN)
    print_endpoint "GET /api/bookings/user/{userId} - Получение бронирований пользователя (ADMIN)"
    # Сначала нужно получить ID пользователя
    USER_BOOKINGS_RESPONSE=$(http_request "GET" "$BOOKINGS_URL/user/1" "" "$ADMIN_TOKEN")
    local user_bookings_status=$?
    if [ $user_bookings_status -eq 200 ]; then
        print_success "Бронирования пользователя получены успешно"
    else
        print_error "Ошибка получения бронирований пользователя. Код: $user_bookings_status"
    fi

    # Отмена бронирования
    print_endpoint "DELETE /api/bookings/{id} - Отмена бронирования"
    CANCEL_RESPONSE=$(http_request "DELETE" "$BOOKINGS_URL/$BOOKING_ID" "" "$USER_TOKEN")
    local cancel_status=$?
    if [ $cancel_status -eq 200 ]; then
        print_success "Бронирование отменено успешно"
    else
        print_error "Ошибка отмены бронирования. Код: $cancel_status"
    fi
    echo
}

# 6. Тестирование User Controller (Admin only)
test_user_controller() {
    print_section "6. ТЕСТИРОВАНИЕ USER CONTROLLER (ADMIN)"

    if [ -z "$ADMIN_TOKEN" ]; then
        print_error "Токен администратора отсутствует"
        return 1
    fi

    # Получение всех пользователей
    print_endpoint "GET /api/admin/users - Получение всех пользователей"
    ALL_USERS_RESPONSE=$(http_request "GET" "$USERS_URL" "" "$ADMIN_TOKEN")
    local all_users_status=$?
    if [ $all_users_status -eq 200 ]; then
        print_success "Все пользователи получены успешно"
        # Сохраняем ID первого пользователя для дальнейших тестов
        USER_ID=$(echo "$ALL_USERS_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_error "Ошибка получения всех пользователей. Код: $all_users_status"
        return 1
    fi

    if [ ! -z "$USER_ID" ]; then
        # Получение пользователя по ID
        print_endpoint "GET /api/admin/users/{id} - Получение пользователя по ID"
        USER_BY_ID_RESPONSE=$(http_request "GET" "$USERS_URL/$USER_ID" "" "$ADMIN_TOKEN")
        local user_by_id_status=$?
        if [ $user_by_id_status -eq 200 ]; then
            print_success "Пользователь по ID получен успешно"
        else
            print_error "Ошибка получения пользователя по ID. Код: $user_by_id_status"
        fi

        # Создание нового пользователя (ADMIN)
        print_endpoint "POST /api/admin/users - Создание пользователя (ADMIN)"
        NEW_USER_RESPONSE=$(http_request "POST" "$USERS_URL" '{
            "username": "newuserbyadmin",
            "password": "password123",
            "email": "newuserbyadmin@example.com",
            "firstName": "New",
            "lastName": "UserByAdmin",
            "role": "USER",
            "active": true
        }' "$ADMIN_TOKEN")

        local new_user_status=$?
        if [ $new_user_status -eq 200 ]; then
            print_success "Новый пользователь создан администратором"
            NEW_USER_ID=$(echo "$NEW_USER_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
        else
            print_error "Ошибка создания пользователя. Код: $new_user_status"
        fi

        # Обновление пользователя
        if [ ! -z "$NEW_USER_ID" ]; then
            print_endpoint "PUT /api/admin/users/{id} - Обновление пользователя"
            UPDATE_USER_RESPONSE=$(http_request "PUT" "$USERS_URL/$NEW_USER_ID" '{
                "username": "updateduser",
                "email": "updateduser@example.com",
                "firstName": "Updated",
                "lastName": "User",
                "role": "USER",
                "active": true
            }' "$ADMIN_TOKEN")

            local update_user_status=$?
            if [ $update_user_status -eq 200 ]; then
                print_success "Пользователь обновлен успешно"
            else
                print_error "Ошибка обновления пользователя. Код: $update_user_status"
            fi

            # Деактивация пользователя
            print_endpoint "PATCH /api/admin/users/{id}/deactivate - Деактивация пользователя"
            DEACTIVATE_RESPONSE=$(http_request "PATCH" "$USERS_URL/$NEW_USER_ID/deactivate" "" "$ADMIN_TOKEN")
            local deactivate_status=$?
            if [ $deactivate_status -eq 200 ]; then
                print_success "Пользователь деактивирован"
            else
                print_error "Ошибка деактивации пользователя. Код: $deactivate_status"
            fi

            # Активация пользователя
            print_endpoint "PATCH /api/admin/users/{id}/activate - Активация пользователя"
            ACTIVATE_RESPONSE=$(http_request "PATCH" "$USERS_URL/$NEW_USER_ID/activate" "" "$ADMIN_TOKEN")
            local activate_status=$?
            if [ $activate_status -eq 200 ]; then
                print_success "Пользователь активирован"
            else
                print_error "Ошибка активации пользователя. Код: $activate_status"
            fi
        fi
    fi
    echo
}

# 7. Тестирование ошибок и security
test_security_and_errors() {
    print_section "7. ТЕСТИРОВАНИЕ БЕЗОПАСНОСТИ И ОШИБОК"

    # Попытка доступа без токена
    print_endpoint "Без токена - доступ к защищенным endpoint'ам"
    NO_TOKEN_RESPONSE=$(http_request "GET" "$HOTELS_URL" "")
    local no_token_status=$?
    if [ $no_token_status -eq 401 ] || [ $no_token_status -eq 403 ]; then
        print_success "Защита от неавторизованного доступа работает. Код: $no_token_status"
    else
        print_error "Неожиданный ответ без токена. Код: $no_token_status"
    fi

    # Попытка доступа пользователя к ADMIN endpoint'ам
    if [ ! -z "$USER_TOKEN" ]; then
        print_endpoint "USER доступ к ADMIN endpoint'ам - создание отеля"
        USER_AS_ADMIN_RESPONSE=$(http_request "POST" "$HOTELS_URL" '{
            "name": "Unauthorized Hotel",
            "address": "Test Address"
        }' "$USER_TOKEN")

        local user_as_admin_status=$?
        if [ $user_as_admin_status -eq 403 ] || [ $user_as_admin_status -eq 401 ]; then
            print_success "Защита ADMIN endpoint'ов работает. Код: $user_as_admin_status"
        else
            print_error "Неожиданный ответ при доступе USER к ADMIN endpoint'у. Код: $user_as_admin_status"
        fi

        print_endpoint "USER доступ к ADMIN endpoint'ам - управление пользователями"
        USER_MANAGE_USERS_RESPONSE=$(http_request "GET" "$USERS_URL" "" "$USER_TOKEN")
        local user_manage_users_status=$?
        if [ $user_manage_users_status -eq 403 ] || [ $user_manage_users_status -eq 401 ]; then
            print_success "Защита User Controller работает. Код: $user_manage_users_status"
        else
            print_error "Неожиданный ответ при доступе USER к User Controller. Код: $user_manage_users_status"
        fi
    fi

    # Тестирование невалидных данных
    print_endpoint "Невалидные данные - создание бронирования с прошедшими датами"
    INVALID_BOOKING_RESPONSE=$(http_request "POST" "$BOOKINGS_URL" '{
        "roomId": '$ROOM_ID',
        "startDate": "2023-01-01",
        "endDate": "2023-01-05"
    }' "$USER_TOKEN")

    local invalid_booking_status=$?
    if [ $invalid_booking_status -eq 400 ]; then
        print_success "Валидация дат работает. Код: $invalid_booking_status"
    else
        print_error "Неожиданный ответ при невалидных датах. Код: $invalid_booking_status"
    fi

    # Тестирование несуществующих ресурсов
    print_endpoint "Несуществующий ресурс - получение отеля с несуществующим ID"
    NOT_FOUND_RESPONSE=$(http_request "GET" "$HOTELS_URL/99999" "" "$USER_TOKEN")
    local not_found_status=$?
    if [ $not_found_status -eq 404 ]; then
        print_success "Обработка 404 ошибок работает. Код: $not_found_status"
    else
        print_error "Неожиданный ответ для несуществующего ресурса. Код: $not_found_status"
    fi
    echo
}

# 8. Проверка Eureka и мониторинга
test_monitoring() {
    print_section "8. ПРОВЕРКА MONITORING И DISCOVERY"

    # Проверка Eureka
    print_endpoint "Eureka Server - проверка доступности"
    if curl -s "http://localhost:8761" > /dev/null; then
        print_success "Eureka Server доступен"

        # Проверка регистрации сервисов
        SERVICES_COUNT=$(curl -s "http://localhost:8761/eureka/apps" | grep -c "<name>")
        if [ $SERVICES_COUNT -gt 0 ]; then
            print_success "В Eureka зарегистрировано сервисов: $SERVICES_COUNT"
        else
            print_error "Сервисы не зарегистрированы в Eureka"
        fi
    else
        print_error "Eureka Server не доступен"
    fi

    # Проверка health endpoints
    print_endpoint "Health Checks - проверка здоровья сервисов"
    services=("auth-service:8081" "api-gateway:8080" "hotel-service:8082" "booking-service:8083")

    for service in "${services[@]}"; do
        IFS=':' read -r name port <<< "$service"
        HEALTH_RESPONSE=$(curl -s "http://localhost:$port/actuator/health")
        if echo "$HEALTH_RESPONSE" | grep -q "\"status\":\"UP\""; then
            print_success "$name: UP"
        else
            print_error "$name: DOWN или не отвечает"
        fi
    done

    # Проверка Swagger UI
    print_endpoint "Swagger UI - проверка документации API"
    if curl -s "http://localhost:8080/swagger-ui.html" | grep -q "swagger-ui" > /dev/null; then
        print_success "Swagger UI доступен"
    else
        print_error "Swagger UI не доступен"
    fi
    echo
}

# 9. Очистка тестовых данных (опционально)
cleanup_test_data() {
    print_section "9. ОЧИСТКА ТЕСТОВЫХ ДАННЫХ"

    if [ ! -z "$ADMIN_TOKEN" ]; then
        # Удаление тестового отеля
        if [ ! -z "$HOTEL_ID" ]; then
            print_endpoint "Удаление тестового отеля"
            DELETE_HOTEL_RESPONSE=$(http_request "DELETE" "$HOTELS_URL/$HOTEL_ID" "" "$ADMIN_TOKEN")
            local delete_hotel_status=$?
            if [ $delete_hotel_status -eq 200 ]; then
                print_success "Тестовый отель удален"
            else
                print_error "Ошибка удаления отеля. Код: $delete_hotel_status"
            fi
        fi

        # Удаление тестового пользователя
        if [ ! -z "$NEW_USER_ID" ]; then
            print_endpoint "Удаление тестового пользователя"
            DELETE_USER_RESPONSE=$(http_request "DELETE" "$USERS_URL/$NEW_USER_ID" "" "$ADMIN_TOKEN")
            local delete_user_status=$?
            if [ $delete_user_status -eq 200 ]; then
                print_success "Тестовый пользователь удален"
            else
                print_error "Ошибка удаления пользователя. Код: $delete_user_status"
            fi
        fi
    fi
    echo
}

# Главная функция
main() {
    echo "НАЧАЛО ПОЛНОГО ТЕСТИРОВАНИЯ СИСТЕМЫ"
    echo "=============================================="
    echo

    check_services_health
    test_auth_controller
    test_hotel_controller
    test_room_controller
    test_booking_controller
    test_user_controller
    test_security_and_errors
    test_monitoring
    # cleanup_test_data  # Раскомментируйте если хотите очистку данных

    echo "=============================================="
    echo "ПОЛНОЕ ТЕСТИРОВАНИЕ ЗАВЕРШЕНО"
    echo

    echo "Сводка тестовых данных:"
    if [ ! -z "$USER_TOKEN" ]; then
        echo "Токен пользователя: ${USER_TOKEN:0:50}..."
    fi
    if [ ! -z "$ADMIN_TOKEN" ]; then
        echo "Токен администратора: ${ADMIN_TOKEN:0:50}..."
    fi
    if [ ! -z "$HOTEL_ID" ]; then
        echo "ID отеля: $HOTEL_ID"
    fi
    if [ ! -z "$ROOM_ID" ]; then
        echo "ID номера: $ROOM_ID"
    fi
    if [ ! -z "$BOOKING_ID" ]; then
        echo "ID бронирования: $BOOKING_ID"
    fi

    echo
    echo "Для дополнительного тестирования используйте:"
    echo "  Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "  Eureka: http://localhost:8761"
    echo "  H2 Console Hotel: http://localhost:8082/h2-console"
    echo "  H2 Console Booking: http://localhost:8083/h2-console"
    echo "  H2 Console Auth: http://localhost:8081/h2-console"
}

# Запуск тестов
main