#!/bin/bash
# setup-and-test.sh

echo "=== Установка и тестирование системы бронирования отелей ==="

# 1. Сборка проекта
echo "1. Сборка проекта..."
mvn clean install -DskipTests

# 2. Запуск сервисов в фоне
echo "2. Запуск сервисов..."
echo "Запуск Eureka Server..."
mvn spring-boot:run -pl eureka-server -Dspring-boot.run.jvmArguments="-Dserver.port=8761" &
EUREKA_PID=$!
sleep 10

echo "Запуск Auth Service..."
mvn spring-boot:run -pl auth-service -Dspring-boot.run.jvmArguments="-Dserver.port=8081" &
AUTH_PID=$!
sleep 10

echo "Запуск Hotel Service..."
mvn spring-boot:run -pl hotel-service -Dspring-boot.run.jvmArguments="-Dserver.port=8082" &
HOTEL_PID=$!
sleep 10

echo "Запуск Booking Service..."
mvn spring-boot:run -pl booking-service -Dspring-boot.run.jvmArguments="-Dserver.port=8083" &
BOOKING_PID=$!
sleep 10

echo "Запуск API Gateway..."
mvn spring-boot:run -pl api-gateway -Dspring-boot.run.jvmArguments="-Dserver.port=8080" &
GATEWAY_PID=$!
sleep 15

# 3. Ожидание запуска всех сервисов
echo "3. Ожидание запуска всех сервисов..."
sleep 30

# 4. Запуск тестов
echo "4. Запуск тестового скрипта..."
chmod +x test-hotel-booking-system.sh
./test-hotel-booking-system.sh

# 5. Остановка сервисов
echo "5. Остановка сервисов..."
kill $EUREKA_PID $AUTH_PID $HOTEL_PID $BOOKING_PID $GATEWAY_PID

echo "=== Тестирование завершено ==="