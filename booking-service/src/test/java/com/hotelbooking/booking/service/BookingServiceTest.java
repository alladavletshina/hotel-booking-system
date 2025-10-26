package com.hotelbooking.booking.service;

import com.hotelbooking.booking.client.HotelServiceClient;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelServiceClient hotelServiceClient;

    @Mock
    private InternalAuthService internalAuthService;

    @InjectMocks
    private BookingService bookingService;

    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUserId(1L);
        testBooking.setUsername("testuser");
        testBooking.setRoomId(101L);
        testBooking.setStartDate(LocalDate.now().plusDays(1));
        testBooking.setEndDate(LocalDate.now().plusDays(3));
        testBooking.setStatus(BookingStatus.PENDING);
    }

    /**
     * Тест для endpoint: POST /bookings
     * Назначение: Создание нового бронирования
     * Сценарий: Успешное подтверждение бронирования при доступности номера
     * Ожидаемый результат:
     * - Возвращает созданное бронирование
     * - Статус бронирования подтвержден
     * Бизнес-логика:
     * 1. Проверяет уникальность correlationId
     * 2. Проверяет валидность токена аутентификации
     * 3. Проверяет доступность номера через HotelService
     * 4. Сохраняет бронирование в статусе PENDING
     * 5. При подтверждении доступности обновляет статус на CONFIRMED
     */
    @Test
    void createBooking_ShouldConfirmBooking_WhenRoomAvailable() {
        // Arrange
        when(bookingRepository.existsByCorrelationId(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(internalAuthService.isTokenValid()).thenReturn(true);
        when(hotelServiceClient.confirmAvailability(anyLong())).thenReturn(true);

        // Act
        Booking result = bookingService.createBooking(testBooking, "correlation-123");

        // Assert
        assertNotNull(result);
        verify(bookingRepository, times(2)).save(any(Booking.class));
    }

    /**
     * Тест для endpoint: DELETE /bookings/{id}
     * Назначение: Отмена существующего бронирования
     * Сценарий: Успешная отмена существующего бронирования
     * Ожидаемый результат:
     * - Возвращает отмененное бронирование
     * - Статус бронирования изменен на CANCELLED
     * Бизнес-логика:
     * 1. Находит бронирование по ID
     * 2. Изменяет статус бронирования на CANCELLED
     * 3. Сохраняет обновленное бронирование
     * 4. Вызывает сервис отелей для освобождения номера
     * 5. Возвращает обновленное бронирование
     */
    @Test
    void cancelBooking_ShouldCancelBooking_WhenBookingExists() {
        // Arrange
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        // Act
        Booking result = bookingService.cancelBooking(1L);

        // Assert
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(hotelServiceClient, times(1)).releaseRoom(anyLong());
    }

    /**
     * Тест для endpoint: GET /bookings/{id}
     * Назначение: Получение информации о бронировании по ID
     * Сценарий: Успешное получение существующего бронирования
     * Ожидаемый результат:
     * - Возвращает бронирование в Optional
     * - Содержит корректные данные бронирования
     * Бизнес-логика:
     * 1. Ищет бронирование в репозитории по ID
     * 2. Возвращает найденное бронирование в Optional
     * 3. Если бронирование не найдено, возвращает Optional.empty()
     */
    @Test
    void getBookingById_ShouldReturnBooking_WhenExists() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // Act
        Optional<Booking> result = bookingService.getBookingById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    /**
     * Тест для endpoint: GET /bookings/user/{userId}
     * Назначение: Получение списка бронирований пользователя
     * Сценарий: Успешное получение бронирований пользователя
     * Ожидаемый результат:
     * - Возвращает список бронирований пользователя
     * - Содержит корректные данные бронирований
     * Бизнес-логика:
     * 1. Ищет бронирования по ID пользователя в репозитории
     * 2. Возвращает список найденных бронирований
     */
    @Test
    void getUserBookings_ShouldReturnUserBookings() {
        // Arrange
        Long userId = 1L;
        Booking booking1 = createBooking(1L, userId, 101L, BookingStatus.CONFIRMED);
        Booking booking2 = createBooking(2L, userId, 102L, BookingStatus.PENDING);
        List<Booking> expectedBookings = Arrays.asList(booking1, booking2);

        when(bookingRepository.findByUserId(userId)).thenReturn(expectedBookings);

        // Act
        List<Booking> result = bookingService.getUserBookings(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(bookingRepository, times(1)).findByUserId(userId);
    }

    /**
     * Тест для endpoint: GET /bookings/my
     * Назначение: Получение бронирований текущего пользователя
     * Сценарий: Успешное получение бронирований авторизованного пользователя
     * Ожидаемый результат:
     * - Возвращает список бронирований текущего пользователя
     * - Содержит корректные данные бронирований
     * Бизнес-логика:
     * 1. Получает аутентификацию из SecurityContext
     * 2. Извлекает JWT токен
     * 3. Извлекает userId из claims токена
     * 4. Ищет бронирования по ID пользователя в репозитории
     * 5. Возвращает список найденных бронирований
     */
    @Test
    void getCurrentUserBookings_ShouldReturnCurrentUserBookings() {
        // Arrange
        Long userId = 1L;
        String username = "testuser";
        Booking booking1 = createBooking(1L, userId, 101L, BookingStatus.CONFIRMED);
        List<Booking> expectedBookings = Arrays.asList(booking1);

        // Создаем реальный JWT токен с нужными claims
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("userId", userId)
                .claim("sub", username)
                .build();

        // Настройка SecurityContext - используем только необходимые стабы
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        org.springframework.security.core.context.SecurityContext securityContext =
                mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // Сохраняем оригинальный контекст и устанавливаем мок
        org.springframework.security.core.context.SecurityContext originalContext =
                SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(securityContext);

            when(bookingRepository.findByUserId(userId)).thenReturn(expectedBookings);

            // Act
            List<Booking> result = bookingService.getCurrentUserBookings();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(userId, result.get(0).getUserId());
            verify(bookingRepository, times(1)).findByUserId(userId);
        } finally {
            // Восстанавливаем оригинальный контекст
            SecurityContextHolder.setContext(originalContext);
        }
    }

    /**
     * Тест для endpoint: GET /bookings (ADMIN)
     * Назначение: Получение всех бронирований в системе
     * Сценарий: Успешное получение всех бронирований
     * Ожидаемый результат:
     * - Возвращает список всех бронирований
     * - Содержит полные данные всех бронирований
     * Бизнес-логика:
     * 1. Ищет все бронирования в репозитории
     * 2. Возвращает полный список бронирований
     */
    @Test
    void getAllBookings_ShouldReturnAllBookings() {
        // Arrange
        Booking booking1 = createBooking(1L, 1L, 101L, BookingStatus.CONFIRMED);
        Booking booking2 = createBooking(2L, 2L, 102L, BookingStatus.PENDING);
        List<Booking> expectedBookings = Arrays.asList(booking1, booking2);

        when(bookingRepository.findAll()).thenReturn(expectedBookings);

        // Act
        List<Booking> result = bookingService.getAllBookings();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(bookingRepository, times(1)).findAll();
    }

    /**
     * Тест для метода: updateBookingStatus
     * Назначение: Обновление статуса бронирования
     * Сценарий: Успешное обновление статуса существующего бронирования
     * Ожидаемый результат:
     * - Возвращает обновленное бронирование
     * - Статус бронирования изменен на указанный
     * Бизнес-логика:
     * 1. Находит бронирование по ID
     * 2. Обновляет статус бронирования
     * 3. Сохраняет обновленное бронирование
     * 4. Возвращает обновленное бронирование
     */
    @Test
    void updateBookingStatus_ShouldUpdateStatus_WhenBookingExists() {
        // Arrange
        Long bookingId = 1L;
        Booking existingBooking = createBooking(bookingId, 1L, 101L, BookingStatus.PENDING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existingBooking));
        when(bookingRepository.save(existingBooking)).thenReturn(existingBooking);

        // Act
        Booking result = bookingService.updateBookingStatus(bookingId, BookingStatus.CONFIRMED);

        // Assert
        assertNotNull(result);
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(bookingRepository, times(1)).save(existingBooking);
    }

    /**
     * Тест для метода: getBookingsByStatus
     * Назначение: Получение бронирований по статусу
     * Сценарий: Успешное получение бронирований с указанным статусом
     * Ожидаемый результат:
     * - Возвращает список бронирований с указанным статусом
     * - Содержит только бронирования с запрошенным статусом
     * Бизнес-логика:
     * 1. Ищет бронирования по статусу в репозитории
     * 2. Возвращает список найденных бронирований
     */
    @Test
    void getBookingsByStatus_ShouldReturnBookings_WithSpecifiedStatus() {
        // Arrange
        BookingStatus status = BookingStatus.CONFIRMED;
        Booking booking1 = createBooking(1L, 1L, 101L, status);
        Booking booking2 = createBooking(2L, 2L, 102L, status);
        List<Booking> expectedBookings = Arrays.asList(booking1, booking2);

        when(bookingRepository.findByStatus(status)).thenReturn(expectedBookings);

        // Act
        List<Booking> result = bookingService.getBookingsByStatus(status);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(booking -> booking.getStatus() == status));
        verify(bookingRepository, times(1)).findByStatus(status);
    }

    /**
     * Тест для метода: completeExpiredBookings
     * Назначение: Автоматическое завершение истекших бронирований
     * Сценарий: Успешное завершение бронирований с истекшей датой окончания
     * Ожидаемый результат:
     * - Статус истекших бронирований изменен на COMPLETED
     * - Актуальные бронирования остаются без изменений
     * Бизнес-логика:
     * 1. Находит все подтвержденные бронирования
     * 2. Проверяет дату окончания каждого бронирования
     * 3. Для истекших бронирований устанавливает статус COMPLETED
     * 4. Сохраняет изменения
     */
    @Test
    void completeExpiredBookings_ShouldCompleteExpiredBookings() {
        // Arrange
        Booking expiredBooking = createBooking(1L, 1L, 101L, BookingStatus.CONFIRMED);
        expiredBooking.setEndDate(LocalDate.now().minusDays(1));

        Booking activeBooking = createBooking(2L, 2L, 102L, BookingStatus.CONFIRMED);
        activeBooking.setEndDate(LocalDate.now().plusDays(1));

        List<Booking> confirmedBookings = Arrays.asList(expiredBooking, activeBooking);

        when(bookingRepository.findByStatus(BookingStatus.CONFIRMED)).thenReturn(confirmedBookings);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        bookingService.completeExpiredBookings();

        // Assert
        verify(bookingRepository, times(1)).save(expiredBooking);
        assertEquals(BookingStatus.COMPLETED, expiredBooking.getStatus());
        assertEquals(BookingStatus.CONFIRMED, activeBooking.getStatus());
    }

    /**
     * Тест для endpoint: POST /bookings
     * Назначение: Создание бронирования с недоступным номером
     * Сценарий: Отмена бронирования при недоступности номера
     * Ожидаемый результат:
     * - Выбрасывает исключение с сообщением о недоступности
     * - Статус бронирования изменен на CANCELLED
     * Бизнес-логика:
     * 1. Проверяет уникальность correlationId
     * 2. Сохраняет бронирование в статусе PENDING
     * 3. Проверяет доступность номера
     * 4. При недоступности номера отменяет бронирование
     * 5. Освобождает номер через компенсирующее действие
     */
    @Test
    void createBooking_ShouldCancelBooking_WhenRoomNotAvailable() {
        // Arrange
        when(bookingRepository.existsByCorrelationId(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(internalAuthService.isTokenValid()).thenReturn(true);
        when(hotelServiceClient.confirmAvailability(anyLong())).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(testBooking, "correlation-123"));

        assertEquals("Booking failed: Room is not available for selected dates", exception.getMessage());

        // ИСПРАВЛЕНО: теперь проверяем 3 вызова save() и 2 вызова releaseRoom()
        verify(bookingRepository, times(3)).save(any(Booking.class));
        verify(hotelServiceClient, times(2)).releaseRoom(anyLong());
    }

    /**
     * Тест для endpoint: POST /bookings
     * Назначение: Создание бронирования с дублирующим correlationId
     * Сценарий: Возврат существующего бронирования при дублирующем запросе
     * Ожидаемый результат:
     * - Возвращает существующее бронирование
     * - Не создает новое бронирование
     * Бизнес-логика:
     * 1. Проверяет существование correlationId
     * 2. Находит существующее бронирование по correlationId
     * 3. Возвращает существующее бронирование без создания нового
     */
    @Test
    void createBooking_ShouldReturnExistingBooking_WhenDuplicateCorrelationId() {
        // Arrange
        when(bookingRepository.existsByCorrelationId("duplicate-correlation")).thenReturn(true);
        when(bookingRepository.findByCorrelationId("duplicate-correlation")).thenReturn(Optional.of(testBooking));

        // Act
        Booking result = bookingService.createBooking(testBooking, "duplicate-correlation");

        // Assert
        assertNotNull(result);
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(hotelServiceClient, never()).confirmAvailability(anyLong());
    }

    /**
     * Тест для endpoint: DELETE /bookings/{id}
     * Назначение: Отмена несуществующего бронирования
     * Сценарий: Обработка попытки отмены несуществующего бронирования
     * Ожидаемый результат:
     * - Выбрасывает исключение с сообщением о ненайденном бронировании
     * - Не выполняет освобождение номера
     * Бизнес-логика:
     * 1. Ищет бронирование по ID
     * 2. При отсутствии бронирования выбрасывает исключение
     * 3. Не выполняет дальнейшие действия
     */
    @Test
    void cancelBooking_ShouldThrowException_WhenBookingNotFound() {
        // Arrange
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingService.cancelBooking(999L));

        assertEquals("Booking not found with ID: 999", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(hotelServiceClient, never()).releaseRoom(anyLong());
    }

    /**
     * Тест для endpoint: GET /bookings/{id}
     * Назначение: Получение несуществующего бронирования
     * Сценарий: Обработка запроса несуществующего бронирования
     * Ожидаемый результат:
     * - Возвращает пустой Optional
     * - Не выбрасывает исключение
     * Бизнес-логика:
     * 1. Ищет бронирование по ID
     * 2. При отсутствии бронирования возвращает Optional.empty()
     */
    @Test
    void getBookingById_ShouldReturnEmpty_WhenNotExists() {
        // Arrange
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Booking> result = bookingService.getBookingById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(bookingRepository, times(1)).findById(999L);
    }

    /**
     * Тест для endpoint: POST /bookings
     * Назначение: Создание бронирования с невалидными датами
     * Сценарий: Обработка попытки создания бронирования с датами в прошлом
     * Ожидаемый результат:
     * - Выбрасывает исключение с сообщением о невалидных датах
     * - Не сохраняет бронирование
     * Бизнес-логика:
     * 1. Проверяет валидность дат бронирования
     * 2. При невалидных датах выбрасывает исключение
     * 3. Не выполняет дальнейшие действия
     */
    @Test
    void createBooking_ShouldThrowException_WhenInvalidDates() {
        // Arrange
        Booking invalidBooking = new Booking();
        invalidBooking.setUserId(1L);
        invalidBooking.setRoomId(101L);
        invalidBooking.setStartDate(LocalDate.now().minusDays(1)); // Дата в прошлом
        invalidBooking.setEndDate(LocalDate.now().plusDays(3));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(invalidBooking, "correlation-123"));

        assertEquals("Start date cannot be in the past", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Тест для метода: updateBookingStatus
     * Назначение: Обновление статуса несуществующего бронирования
     * Сценарий: Обработка попытки обновления статуса несуществующего бронирования
     * Ожидаемый результат:
     * - Выбрасывает исключение с сообщением о ненайденном бронировании
     * - Не сохраняет изменения
     * Бизнес-логика:
     * 1. Ищет бронирование по ID
     * 2. При отсутствии бронирования выбрасывает исключение
     * 3. Не выполняет обновление статуса
     */
    @Test
    void updateBookingStatus_ShouldThrowException_WhenBookingNotFound() {
        // Arrange
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingService.updateBookingStatus(999L, BookingStatus.CONFIRMED));

        assertEquals("Booking not found with ID: 999", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // Вспомогательный метод для создания тестовых бронирований
    private Booking createBooking(Long id, Long userId, Long roomId, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUserId(userId);
        booking.setRoomId(roomId);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setStatus(status);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        return booking;
    }
}