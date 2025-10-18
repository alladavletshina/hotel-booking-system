#!/bin/bash

echo "🧪 Testing Hotel Controller API"
echo "================================"

BASE_URL="http://localhost:8080/api/hotels"

# 1. Получить все отели
echo -e "\n1. 🔍 GET /api/hotels"
curl -s $BASE_URL

# 2. Создать отель 1
echo -e "\n\n2. ➕ POST /api/hotels - Отель 1"
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Grand Hotel",
    "address": "Moscow, Red Square 1",
    "description": "Luxury 5-star hotel"
  }'

# 3. Создать отель 2
echo -e "\n\n3. ➕ POST /api/hotels - Отель 2"
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Seaside Resort",
    "address": "Sochi, Beach Boulevard 25",
    "description": "Beachfront resort with spa"
  }'

# 4. Получить все отели (должны быть 2)
echo -e "\n\n4. 🔍 GET /api/hotels (должны быть 2 отеля)"
curl -s $BASE_URL

# 5. Получить отель по ID 1
echo -e "\n\n5. 🔍 GET /api/hotels/1"
curl -s $BASE_URL/1

# 6. Обновить отель 1
echo -e "\n\n6. ✏️ PUT /api/hotels/1"
curl -s -X PUT $BASE_URL/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GRAND HOTEL UPDATED",
    "address": "Moscow, Red Square 1 - RENOVATED",
    "description": "Recently renovated luxury hotel"
  }'

# 7. Проверить обновление
echo -e "\n\n7. 🔍 GET /api/hotels/1 (проверить обновление)"
curl -s $BASE_URL/1

# 8. Удалить отель 2
echo -e "\n\n8. 🗑️ DELETE /api/hotels/2"
curl -s -X DELETE $BASE_URL/2

# 9. Проверить что отель 2 удален
echo -e "\n\n9. 🔍 GET /api/hotels/2 (должен быть 404)"
curl -s -w "Status: %{http_code}\n" $BASE_URL/2

# 10. Финальный список отелей
echo -e "\n\n10. 🔍 GET /api/hotels (финальный список)"
curl -s $BASE_URL

echo -e "\n\n🎉 Тестирование завершено!"chmod +x test-hotel-controller.sh