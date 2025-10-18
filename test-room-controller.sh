#!/bin/bash

echo "🧪 Testing Room Controller API"
echo "================================"

BASE_URL="http://localhost:8080/api/rooms"

# 1. Получить все доступные комнаты
echo -e "\n1. 🔍 GET /api/rooms - Все доступные комнаты"
curl -s $BASE_URL

# 2. Создать отель для тестирования комнат
echo -e "\n\n2. 🏨 Создаем отель для тестирования"
HOTEL_RESPONSE=$(curl -s -X POST http://localhost:8080/api/hotels \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Hotel for Rooms",
    "address": "Test Address for Rooms",
    "description": "Hotel for room testing"
  }')

echo "Создан отель: $HOTEL_RESPONSE"

# Извлекаем ID отеля
HOTEL_ID=$(echo $HOTEL_RESPONSE | grep -o '"id":[0-9]*' | cut -d: -f2)
echo "Hotel ID: $HOTEL_ID"

# 3. Создать комнату 1
echo -e "\n\n3. ➕ POST /api/rooms - Комната 1"
ROOM1_RESPONSE=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "number": "101",
    "type": "Standard",
    "price": 100.0,
    "available": true,
    "hotelId": '$HOTEL_ID'
  }')

echo "Создана комната: $ROOM1_RESPONSE"

# Извлекаем ID комнаты 1
ROOM1_ID=$(echo $ROOM1_RESPONSE | grep -o '"id":[0-9]*' | cut -d: -f2)
echo "Room 1 ID: $ROOM1_ID"

# 4. Создать комнату 2
echo -e "\n\n4. ➕ POST /api/rooms - Комната 2"
ROOM2_RESPONSE=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "number": "102",
    "type": "Deluxe",
    "price": 200.0,
    "available": true,
    "hotelId": '$HOTEL_ID'
  }')

echo "Создана комната: $ROOM2_RESPONSE"

# Извлекаем ID комнаты 2
ROOM2_ID=$(echo $ROOM2_RESPONSE | grep -o '"id":[0-9]*' | cut -d: -f2)
echo "Room 2 ID: $ROOM2_ID"

# 5. Создать недоступную комнату
echo -e "\n\n5. ➕ POST /api/rooms - Недоступная комната"
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "number": "103",
    "type": "Suite",
    "price": 300.0,
    "available": false,
    "hotelId": '$HOTEL_ID'
  }'

# 6. Получить все доступные комнаты (должны быть 2)
echo -e "\n\n6. 🔍 GET /api/rooms - Доступные комнаты (должны быть 2)"
curl -s $BASE_URL

# 7. Получить комнату по ID
echo -e "\n\n7. 🔍 GET /api/rooms/{id} - Комната $ROOM1_ID"
curl -s $BASE_URL/$ROOM1_ID

# 8. Получить рекомендованные комнаты
echo -e "\n\n8. 🔍 GET /api/rooms/recommend - Рекомендованные комнаты"
curl -s $BASE_URL/recommend

# 9. Подтвердить доступность комнаты
echo -e "\n\n9. ✅ POST /api/rooms/{id}/confirm-availability - Подтвердить доступность комнаты $ROOM1_ID"
curl -s -X POST $BASE_URL/$ROOM1_ID/confirm-availability

# 10. Проверить что timesBooked увеличился
echo -e "\n\n10. 🔍 GET /api/rooms/$ROOM1_ID - Проверить timesBooked"
curl -s $BASE_URL/$ROOM1_ID

# 11. Освободить комнату
echo -e "\n\n11. 🔓 POST /api/rooms/{id}/release - Освободить комнату $ROOM1_ID"
curl -s -X POST $BASE_URL/$ROOM1_ID/release

# 12. Проверить что timesBooked уменьшился
echo -e "\n\n12. 🔍 GET /api/rooms/$ROOM1_ID - Проверить timesBooked после release"
curl -s $BASE_URL/$ROOM1_ID

# 13. Удалить комнату
echo -e "\n\n13. 🗑️ DELETE /api/rooms/{id} - Удалить комнату $ROOM2_ID"
curl -s -X DELETE $BASE_URL/$ROOM2_ID

# 14. Проверить что комната удалена
echo -e "\n\n14. 🔍 GET /api/rooms/$ROOM2_ID - Проверить удаление (должен быть 404)"
curl -s -w "Status: %{http_code}\n" $BASE_URL/$ROOM2_ID

# 15. Финальный список доступных комнат
echo -e "\n\n15. 🔍 GET /api/rooms - Финальный список доступных комнат"
curl -s $BASE_URL

# 16. Тестирование ошибок
echo -e "\n\n16. 🐛 Тестирование ошибок"

# Несуществующая комната
echo -e "\n- Несуществующая комната:"
curl -s -w "Status: %{http_code}\n" $BASE_URL/999

# Подтверждение доступности несуществующей комнаты
echo -e "\n- Подтверждение доступности несуществующей комнаты:"
curl -s -X POST -w "Status: %{http_code}\n" $BASE_URL/999/confirm-availability

echo -e "\n\n🎉 Тестирование Room Controller завершено!"