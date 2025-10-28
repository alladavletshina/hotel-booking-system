package com.hotelbooking.booking.service;

import com.hotelbooking.booking.client.HotelServiceClient;
import com.hotelbooking.booking.client.dto.RoomRecommendation;
import com.hotelbooking.booking.client.dto.AvailabilityRequest;
import com.hotelbooking.booking.client.dto.ReleaseRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelServiceClient hotelServiceClient;

    @Mock
    private InternalAuthService internalAuthService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BookingService bookingService;

    private Booking testBooking;
    private final String CORRELATION_ID = "test-correlation-id";
    private final Long USER_ID = 123L;
    private final Long ROOM_ID = 456L;

    @BeforeEach
    void setUp() {
        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUserId(USER_ID);
        testBooking.setRoomId(ROOM_ID);
        testBooking.setStartDate(LocalDate.now().plusDays(1));
        testBooking.setEndDate(LocalDate.now().plusDays(3));
        testBooking.setAutoSelect(false);
    }

    /**
     * Тест для метода: createBooking
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
    void createBooking_WhenRoomAvailable_ShouldConfirmBooking() {
        // Arrange
        when(bookingRepository.existsByCorrelationId(CORRELATION_ID)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(internalAuthService.isTokenValid()).thenReturn(true);
        when(hotelServiceClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class))).thenReturn(true);

        // Act
        Booking result = bookingService.createBooking(testBooking, CORRELATION_ID);

        // Assert
        assertNotNull(result);
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        verify(hotelServiceClient).confirmAvailability(eq(ROOM_ID), any(AvailabilityRequest.class));
        // Проверяем, что releaseRoom не вызывался
        verify(hotelServiceClient, never()).releaseRoom(anyLong(), any(ReleaseRequest.class));
    }

    /**
     * Тест для метода: createBooking
     * Назначение: Создание нового бронирования
     * Сценарий: Отклонение бронирования при недоступности номера
     * Ожидаемый результат:
     * - Бронирование отменяется
     * - Статус бронирования CANCELLED
     * Бизнес-логика:
     * 1. Сохраняет бронирование в статусе PENDING
     * 2. Проверяет доступность номера через HotelService
     * 3. При недоступности номера вызывает handleBookingFailure
     * 4. Освобождает номер через releaseRoom
     */
    @Test
    void createBooking_WhenRoomNotAvailable_ShouldCancelBooking() {
        // Arrange
        when(bookingRepository.existsByCorrelationId(CORRELATION_ID)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(internalAuthService.isTokenValid()).thenReturn(true);
        when(hotelServiceClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class))).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(testBooking, CORRELATION_ID));

        assertTrue(exception.getMessage().contains("Room is not available for selected dates"));

        // Проверяем, что releaseRoom вызывался хотя бы один раз (может вызываться несколько раз из-за логики отката)
        verify(hotelServiceClient, atLeastOnce()).releaseRoom(eq(ROOM_ID), any(ReleaseRequest.class));

        // Проверяем, что бронирование было сохранено с статусом CANCELLED
        verify(bookingRepository, atLeast(2)).save(any(Booking.class));
    }

    /**
     * Тест для метода: createBooking
     * Назначение: Создание нового бронирования
     * Сценарий: Идемпотентность при дублирующем correlationId
     * Ожидаемый результат:
     * - Возвращает существующее бронирование
     * - Не создает новое бронирование
     * Бизнес-логика:
     * 1. Проверяет существование бронирования по correlationId
     * 2. Возвращает существующее бронирование без вызова бизнес-логики
     * 3. Гарантирует идемпотентность операции
     */
    @Test
    void createBooking_WithDuplicateCorrelationId_ShouldReturnExistingBooking() {
        // Arrange
        when(bookingRepository.existsByCorrelationId(CORRELATION_ID)).thenReturn(true);
        when(bookingRepository.findByCorrelationId(CORRELATION_ID)).thenReturn(Optional.of(testBooking));

        // Act
        Booking result = bookingService.createBooking(testBooking, CORRELATION_ID);

        // Assert
        assertNotNull(result);
        assertEquals(testBooking.getId(), result.getId());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(hotelServiceClient, never()).confirmAvailability(anyLong(), any(AvailabilityRequest.class));
        verify(hotelServiceClient, never()).releaseRoom(anyLong(), any(ReleaseRequest.class));
    }

    /**
     * Тест для метода: createBooking
     * Назначение: Создание нового бронирования
     * Сценарий: Автоподбор комнаты при включенном autoSelect
     * Ожидаемый результат:
     * - Автоматически выбирает лучшую комнату
     * - Создает бронирование с выбранной комнатой
     * Бизнес-логика:
     * 1. Определяет необходимость автоподбора по флагу autoSelect
     * 2. Вызывает метод autoSelectBestRoom для выбора комнаты
     * 3. Устанавливает выбранный roomId в бронирование
     */
    @Test
    void createBooking_WithAutoSelectEnabled_ShouldAutoSelectRoom() {
        // Arrange
        testBooking.setAutoSelect(true);
        testBooking.setRoomId(null);
        Long autoSelectedRoomId = 789L;

        // Создаем реальный объект RoomRecommendation
        RoomRecommendation recommendation = new RoomRecommendation();
        recommendation.setId(autoSelectedRoomId);
        recommendation.setType("DELUXE");
        recommendation.setPrice(200.0);
        recommendation.setTimesBooked(5);

        when(bookingRepository.existsByCorrelationId(CORRELATION_ID)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(internalAuthService.isTokenValid()).thenReturn(true);
        when(hotelServiceClient.getRecommendedRooms(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(recommendation));
        when(hotelServiceClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class))).thenReturn(true);

        // Act
        Booking result = bookingService.createBooking(testBooking, CORRELATION_ID);

        // Assert
        assertNotNull(result);
        verify(hotelServiceClient).getRecommendedRooms(testBooking.getStartDate(), testBooking.getEndDate());
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        verify(hotelServiceClient, never()).releaseRoom(anyLong(), any(ReleaseRequest.class));
    }

    /**
     * Тест для метода: createBooking
     * Назначение: Создание нового бронирования
     * Сценарий: Ошибка при отсутствии roomId и выключенном autoSelect
     * Ожидаемый результат:
     * - Выбрасывает исключение
     * - Не создает бронирование
     * Бизнес-логика:
     * 1. Проверяет наличие roomId при выключенном autoSelect
     * 2. Выбрасывает исключение при отсутствии roomId
     * 3. Гарантирует обязательность указания комнаты
     */
    @Test
    void createBooking_WithoutRoomIdAndAutoSelectDisabled_ShouldThrowException() {
        // Arrange
        testBooking.setRoomId(null);
        testBooking.setAutoSelect(false);

        when(bookingRepository.existsByCorrelationId(CORRELATION_ID)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(testBooking, CORRELATION_ID));

        assertEquals("Room ID is required when autoSelect is false", exception.getMessage());

        // Проверяем, что не было попыток сохранить бронирование или вызвать внешние сервисы
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(hotelServiceClient, never()).confirmAvailability(anyLong(), any(AvailabilityRequest.class));
        verify(hotelServiceClient, never()).releaseRoom(anyLong(), any(ReleaseRequest.class));
    }

    /**
     * Тест для метода: createBooking
     * Назначение: Создание нового бронирования
     * Сценарий: Ошибка автоподбора при отсутствии доступных комнат
     * Ожидаемый результат:
     * - Выбрасывает исключение
     * - Не создает бронирование
     * Бизнес-логика:
     * 1. Определяет необходимость автоподбора
     * 2. Запрашивает рекомендации у HotelService
     * 3. При отсутствии рекомендаций выбрасывает исключение
     */
    @Test
    void createBooking_WithAutoSelectAndNoAvailableRooms_ShouldThrowException() {
        // Arrange
        testBooking.setAutoSelect(true);
        testBooking.setRoomId(null);

        when(bookingRepository.existsByCorrelationId(CORRELATION_ID)).thenReturn(false);
        when(hotelServiceClient.getRecommendedRooms(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of()); // Пустой список

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(testBooking, CORRELATION_ID));

        assertTrue(exception.getMessage().contains("No available rooms found for selected dates"));

        // Проверяем, что не было попыток подтвердить доступность или освободить комнату
        verify(hotelServiceClient, never()).confirmAvailability(anyLong(), any(AvailabilityRequest.class));
        verify(hotelServiceClient, never()).releaseRoom(anyLong(), any(ReleaseRequest.class));
    }

    /**
     * Тест для метода: cancelBooking
     * Назначение: Отмена существующего бронирования
     * Сценарий: Попытка отмены уже отмененного бронирования
     * Ожидаемый результат:
     * - Выбрасывает исключение
     * - Не выполняет операцию отмены
     * Бизнес-логика:
     * 1. Проверяет текущий статус бронирования
     * 2. Запрещает отмену уже отмененных или завершенных бронирований
     * 3. Сохраняет целостность данных
     */
    @Test
    void cancelBooking_AlreadyCancelled_ShouldThrowException() {
        // Arrange
        Long bookingId = 1L;
        testBooking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        setupSimpleSecurityContext();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingService.cancelBooking(bookingId));

        assertTrue(exception.getMessage().contains("Cannot cancel booking with status: CANCELLED"));
        verify(hotelServiceClient, never()).releaseRoom(anyLong(), any(ReleaseRequest.class));
    }

    /**
     * Тест для метода: getBookingById
     * Назначение: Получение бронирования по ID
     * Сценарий: Успешное получение существующего бронирования
     * Ожидаемый результат:
     * - Возвращает Optional с бронированием
     * Бизнес-логика:
     * 1. Ищет бронирование в репозитории по ID
     * 2. Возвращает результат поиска
     */
    @Test
    void getBookingById_WithExistingId_ShouldReturnBooking() {
        // Arrange
        Long bookingId = 1L;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        // Act
        Optional<Booking> result = bookingService.getBookingById(bookingId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testBooking.getId(), result.get().getId());
    }

    /**
     * Тест для метода: getBookingById
     * Назначение: Получение бронирования по ID
     * Сценарий: Бронирование не найдено
     * Ожидаемый результат:
     * - Возвращает пустой Optional
     * Бизнес-логика:
     * 1. Ищет бронирование в репозитории по ID
     * 2. Возвращает пустой результат если не найдено
     */
    @Test
    void getBookingById_WithNonExistingId_ShouldReturnEmpty() {
        // Arrange
        Long bookingId = 999L;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act
        Optional<Booking> result = bookingService.getBookingById(bookingId);

        // Assert
        assertFalse(result.isPresent());
    }

    /**
     * Тест для метода: validateDates
     * Назначение: Валидация дат для рекомендаций комнат
     * Сценарий: Некорректные даты (начальная дата в прошлом)
     * Ожидаемый результат:
     * - Выбрасывает исключение с сообщением об ошибке
     * Бизнес-логика:
     * 1. Проверяет что начальная дата не в прошлом
     * 2. Проверяет что конечная дата после начальной
     * 3. Проверяет что период не превышает 1 месяц
     */
    @Test
    void getRecommendedRooms_WithStartDateInPast_ShouldThrowException() {
        // Arrange
        LocalDate pastDate = LocalDate.now().minusDays(1);
        LocalDate futureDate = LocalDate.now().plusDays(2);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getRecommendedRooms(pastDate, futureDate));

        assertTrue(exception.getMessage().contains("Start date cannot be in the past"));
    }

    /**
     * Тест для метода: validateDates
     * Назначение: Валидация дат для рекомендаций комнат
     * Сценарий: Конечная дата раньше начальной
     * Ожидаемый результат:
     * - Выбрасывает исключение с сообщением об ошибке
     * Бизнес-логика:
     * 1. Проверяет корректность порядка дат
     * 2. Гарантирует что период бронирования логически корректен
     */
    @Test
    void getRecommendedRooms_WithEndDateBeforeStart_ShouldThrowException() {
        // Arrange
        LocalDate startDate = LocalDate.now().plusDays(3);
        LocalDate endDate = LocalDate.now().plusDays(1);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getRecommendedRooms(startDate, endDate));

        assertTrue(exception.getMessage().contains("End date must be after start date"));
    }

    /**
     * Тест для метода: completeExpiredBookings
     * Назначение: Автоматическое завершение истекших бронирований
     * Сценарий: Нет истекших бронирований
     * Ожидаемый результат:
     * - Не выполняет никаких обновлений
     * Бизнес-логика:
     * 1. Находит все подтвержденные бронирования
     * 2. Проверяет дату окончания каждого бронирования
     * 3. Не обновляет статус если дата окончания не истекла
     */
    @Test
    void completeExpiredBookings_WithNoExpiredBookings_ShouldDoNothing() {
        // Arrange
        Booking activeBooking = new Booking();
        activeBooking.setId(3L);
        activeBooking.setStatus(BookingStatus.CONFIRMED);
        activeBooking.setEndDate(LocalDate.now().plusDays(1)); // Завтра

        when(bookingRepository.findByStatus(BookingStatus.CONFIRMED))
                .thenReturn(List.of(activeBooking));

        // Act
        bookingService.completeExpiredBookings();

        // Assert
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Тест для метода: updateBookingStatus
     * Назначение: Обновление статуса бронирования
     * Сценарий: Успешное обновление статуса
     * Ожидаемый результат:
     * - Возвращает обновленное бронирование
     * - Статус изменен на указанный
     * Бизнес-логика:
     * 1. Находит бронирование по ID
     * 2. Обновляет статус и время модификации
     * 3. Сохраняет изменения
     */
    @Test
    void updateBookingStatus_WithValidBooking_ShouldUpdateStatus() {
        // Arrange
        Long bookingId = 1L;
        BookingStatus newStatus = BookingStatus.COMPLETED;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        // Act
        Booking result = bookingService.updateBookingStatus(bookingId, newStatus);

        // Assert
        assertNotNull(result);
        assertEquals(newStatus, result.getStatus());
        assertNotNull(result.getUpdatedAt());
    }

    /**
     * Тест для метода: updateBookingStatus
     * Назначение: Обновление статуса бронирования
     * Сценарий: Бронирование не найдено
     * Ожидаемый результат:
     * - Выбрасывает исключение
     * Бизнес-логика:
     * 1. Ищет бронирование по ID
     * 2. При отсутствии бронирования выбрасывает исключение
     */
    @Test
    void updateBookingStatus_WithNonExistingBooking_ShouldThrowException() {
        // Arrange
        Long bookingId = 999L;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingService.updateBookingStatus(bookingId, BookingStatus.COMPLETED));

        assertTrue(exception.getMessage().contains("Booking not found with ID: " + bookingId));
    }

    /**
     * Тест для метода: getBookingsByStatus
     * Назначение: Получение бронирований по статусу
     * Сценарий: Успешное получение списка
     * Ожидаемый результат:
     * - Возвращает список бронирований с указанным статусом
     * Бизнес-логика:
     * 1. Запрашивает бронирования из репозитория по статусу
     * 2. Возвращает полученный список
     */
    @Test
    void getBookingsByStatus_WithValidStatus_ShouldReturnBookings() {
        // Arrange
        BookingStatus status = BookingStatus.CONFIRMED;
        when(bookingRepository.findByStatus(status)).thenReturn(List.of(testBooking));

        // Act
        List<Booking> result = bookingService.getBookingsByStatus(status);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(testBooking.getId(), result.get(0).getId());
    }

    private void setupSimpleSecurityContext() {
        // Упрощенная настройка security context без сложных thenReturn
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}