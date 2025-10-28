Hotel Booking System 🏨
Микросервисная система бронирования отелей с Spring Boot и Spring Cloud.

🚀 Быстрый старт
Предварительные требования
* Java 17
* Maven 3.6+
* Порты 8761, 8080-8083

Запуск системы
bash
# Клонирование и сборка
`git clone <repository-url>
cd hotel-booking-system
mvn clean install`

# Запуск сервисов (в разных терминалах)
* mvn spring-boot:run -pl eureka-server
* mvn spring-boot:run -pl auth-service
* mvn spring-boot:run -pl hotel-service
* mvn spring-boot:run -pl booking-service
* mvn spring-boot:run -pl api-gateway

📚 Документация API (Swagger)
После запуска доступна автоматическая документация:

* Auth Service: http://localhost:8081/swagger-ui.html
* Hotel Service: http://localhost:8082/swagger-ui.html
* Booking Service: http://localhost:8083/swagger-ui.html

🔐 Аутентификация
Система использует JWT токены

Получите токен:

`
curl -X POST http://localhost:8081/auth/login \
-H "Content-Type: application/json" \
-d '{"username": "admin", "password": "admin123"}'
Используйте токен в заголовках:`

Authorization: Bearer <your-jwt-token>

👥 Предустановленные пользователи
Администратор: admin / admin123 (роль: ADMIN)

Сервисный пользователь: internal-service / internal-secret-123 (роль: INTERNAL)

🏗 Архитектура

1. [ ] API Gateway (8080)
2. [ ] │
3. [ ] ├── Auth Service (8081) - аутентификация
4. [ ] ├── Hotel Service (8082) - отели и номера
5. [ ] ├── Booking Service (8083) - бронирования
6. [ ] └── Eureka Server (8761) - service discovery


#  Тестирование

Запуск всех тестов
`mvn test`

# Тесты конкретного сервиса
* mvn test -pl auth-service
* mvn test -pl hotel-service
* mvn test -pl booking-service

💡 Бизнес-логика
* Равномерное распределение номеров (наименее популярные первыми)
* Saga pattern для согласованности бронирований
* Автоподбор номеров при autoSelect: true
* Ролевая модель: USER, ADMIN, INTERNAL

🐛 Устранение неполадок
Сервисы не регистрируются в Eureka:

* Проверьте порт 8761
* Убедитесь в правильности credentials

Ошибки аутентификации:

* Проверьте JWT токен
* Убедитесь в корректности ролей

Межсервисные ошибки:

* Проверьте настройки Feign клиентов
* Убедитесь в доступности сервисов

📞 Контакты
Для вопросов и поддержки обращайтесь к Давлетшина Алла

Версия: 1.0.0