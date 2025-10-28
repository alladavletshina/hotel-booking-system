package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.client.dto.RoomRecommendation;
import com.hotelbooking.booking.dto.BookingDto;
import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.mapper.BookingMapper;
import com.hotelbooking.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingController bookingController;

    private BookingRequest bookingRequest;
    private Booking booking;
    private BookingDto bookingDto;
    private final Long USER_ID = 123L;
    private final Long BOOKING_ID = 1L;

    @BeforeEach
    void setUp() {
        bookingRequest = new BookingRequest();
        bookingRequest.setUserId(USER_ID);
        bookingRequest.setRoomId(456L);
        bookingRequest.setStartDate(LocalDate.now().plusDays(1));
        bookingRequest.setEndDate(LocalDate.now().plusDays(3));
        bookingRequest.setAutoSelect(false);

        booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setUserId(USER_ID);
        booking.setRoomId(456L);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setStatus(BookingStatus.CONFIRMED);

        bookingDto = new BookingDto();
        bookingDto.setId(BOOKING_ID);
        bookingDto.setUserId(USER_ID);
        bookingDto.setRoomId(456L);
        bookingDto.setStartDate(LocalDate.now().plusDays(1));
        bookingDto.setEndDate(LocalDate.now().plusDays(3));
        bookingDto.setStatus(BookingStatus.CONFIRMED);
    }

    /**
     * Тест для endpoint: POST /bookings
     * Назначение: Создание нового бронирования
     * Сценарий: Успешное создание бронирования пользователем
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает созданное бронирование в DTO формате
     * Бизнес-логика:
     * 1. Преобразует BookingRequest в сущность Booking
     * 2. Генерирует correlationId если не предоставлен
     * 3. Вызывает сервис для создания бронирования
     * 4. Преобразует результат в DTO и возвращает
     */
    @Test
    void createBooking_WithValidRequest_ShouldReturnBookingDto() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        when(bookingMapper.toEntity(bookingRequest)).thenReturn(booking);
        when(bookingService.createBooking(any(Booking.class), any(String.class))).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);

        // Act
        ResponseEntity<BookingDto> response = bookingController.createBooking(bookingRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BOOKING_ID, response.getBody().getId());
        assertEquals(BookingStatus.CONFIRMED, response.getBody().getStatus());

        verify(bookingMapper).toEntity(bookingRequest);
        verify(bookingService).createBooking(any(Booking.class), any(String.class));
        verify(bookingMapper).toDto(booking);
    }

    /**
     * Тест для endpoint: POST /bookings
     * Назначение: Создание нового бронирования
     * Сценарий: Создание бронирования с предоставленным correlationId
     * Ожидаемый результат:
     * - Использует предоставленный correlationId
     * - Корректно обрабатывает запрос
     * Бизнес-логика:
     * 1. Проверяет наличие correlationId в запросе
     * 2. Использует предоставленный correlationId вместо генерации нового
     * 3. Сохраняет идемпотентность операции
     */
    @Test
    void createBooking_WithCorrelationId_ShouldUseProvidedCorrelationId() {
        // Arrange
        String correlationId = "test-correlation-id";
        bookingRequest.setCorrelationId(correlationId);
        setupUserAuthentication("ROLE_USER");

        when(bookingMapper.toEntity(bookingRequest)).thenReturn(booking);
        when(bookingService.createBooking(any(Booking.class), eq(correlationId))).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);

        // Act
        ResponseEntity<BookingDto> response = bookingController.createBooking(bookingRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).createBooking(any(Booking.class), eq(correlationId));
    }

    /**
     * Тест для endpoint: POST /bookings
     * Назначение: Создание нового бронирования с автоподбором комнаты
     * Сценарий: Автоподбор комнаты при включенном autoSelect
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Создает бронирование с автоматически выбранной комнатой
     * Бизнес-логика:
     * 1. Определяет необходимость автоподбора по флагу autoSelect в запросе
     * 2. Преобразует запрос в сущность с autoSelect = true
     * 3. Вызывает сервис для создания бронирования с автоподбором
     * 4. Возвращает бронирование с выбранной комнатой
     */
    @Test
    void createBooking_WithAutoSelectEnabled_ShouldAutoSelectRoom() {
        // Arrange
        setupUserAuthentication("ROLE_USER");

        // Настраиваем запрос с автоподбором
        bookingRequest.setAutoSelect(true);
        bookingRequest.setRoomId(null); // roomId не указан - будет автоподбор

        // Настраиваем сущность с автоподбором
        Booking bookingWithAutoSelect = new Booking();
        bookingWithAutoSelect.setId(BOOKING_ID);
        bookingWithAutoSelect.setUserId(USER_ID);
        bookingWithAutoSelect.setRoomId(789L); // Автоматически выбранная комната
        bookingWithAutoSelect.setStartDate(LocalDate.now().plusDays(1));
        bookingWithAutoSelect.setEndDate(LocalDate.now().plusDays(3));
        bookingWithAutoSelect.setAutoSelect(true);
        bookingWithAutoSelect.setStatus(BookingStatus.CONFIRMED);

        // Настраиваем DTO с автоподобранной комнатой
        BookingDto bookingDtoWithAutoSelect = new BookingDto();
        bookingDtoWithAutoSelect.setId(BOOKING_ID);
        bookingDtoWithAutoSelect.setUserId(USER_ID);
        bookingDtoWithAutoSelect.setRoomId(789L); // Автоматически выбранная комната
        bookingDtoWithAutoSelect.setStartDate(LocalDate.now().plusDays(1));
        bookingDtoWithAutoSelect.setEndDate(LocalDate.now().plusDays(3));
        bookingDtoWithAutoSelect.setAutoSelect(true);
        bookingDtoWithAutoSelect.setStatus(BookingStatus.CONFIRMED);

        when(bookingMapper.toEntity(bookingRequest)).thenReturn(bookingWithAutoSelect);
        when(bookingService.createBooking(any(Booking.class), any(String.class))).thenReturn(bookingWithAutoSelect);
        when(bookingMapper.toDto(bookingWithAutoSelect)).thenReturn(bookingDtoWithAutoSelect);

        // Act
        ResponseEntity<BookingDto> response = bookingController.createBooking(bookingRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Проверяем, что бронирование создано с автоподобранной комнатой
        assertEquals(789L, response.getBody().getRoomId());
        assertTrue(response.getBody().getAutoSelect());
        assertEquals(BookingStatus.CONFIRMED, response.getBody().getStatus());

        verify(bookingMapper).toEntity(bookingRequest);
        verify(bookingService).createBooking(any(Booking.class), any(String.class));
        verify(bookingMapper).toDto(bookingWithAutoSelect);
    }

    /**
     * Тест для endpoint: POST /bookings
     * Назначение: Создание нового бронирования с автоподбором комнаты
     * Сценарий: Ошибка автоподбора при отсутствии доступных комнат
     * Ожидаемый результат:
     * - Исключение пробрасывается из контроллера
     * - Сообщение об ошибке указывает на проблему автоподбора
     * Бизнес-логика:
     * 1. Определяет необходимость автоподбора
     * 2. Вызывает сервис который выбрасывает исключение при отсутствии комнат
     * 3. Пробрасывает исключение для обработки глобальным обработчиком
     */
    @Test
    void createBooking_WithAutoSelectAndNoRooms_ShouldThrowException() {
        // Arrange
        setupUserAuthentication("ROLE_USER");

        // Настраиваем запрос с автоподбором
        bookingRequest.setAutoSelect(true);
        bookingRequest.setRoomId(null);

        Booking bookingWithAutoSelect = new Booking();
        bookingWithAutoSelect.setAutoSelect(true);
        bookingWithAutoSelect.setRoomId(null);

        when(bookingMapper.toEntity(bookingRequest)).thenReturn(bookingWithAutoSelect);
        when(bookingService.createBooking(any(Booking.class), any(String.class)))
                .thenThrow(new RuntimeException("No available rooms found for selected dates"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingController.createBooking(bookingRequest));

        assertTrue(exception.getMessage().contains("No available rooms"));

        verify(bookingMapper).toEntity(bookingRequest);
        verify(bookingService).createBooking(any(Booking.class), any(String.class));
        verify(bookingMapper, never()).toDto(any(Booking.class));
    }

    /**
     * Тест для endpoint: GET /bookings/user/{userId}
     * Назначение: Получение бронирований пользователя
     * Сценарий: Успешное получение списка бронирований пользователя
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает список бронирований в DTO формате
     * Бизнес-логика:
     * 1. Проверяет права доступа (пользователь или администратор)
     * 2. Получает бронирования по userId из сервиса
     * 3. Преобразует список сущностей в список DTO
     * 4. Возвращает результат
     */
    @Test
    void getUserBookings_WithValidUserId_ShouldReturnBookingsList() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        List<Booking> bookings = Arrays.asList(booking);
        List<BookingDto> bookingDtos = Arrays.asList(bookingDto);

        when(bookingService.getUserBookings(USER_ID)).thenReturn(bookings);
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);

        // Act
        ResponseEntity<List<BookingDto>> response = bookingController.getUserBookings(USER_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(BOOKING_ID, response.getBody().get(0).getId());

        verify(bookingService).getUserBookings(USER_ID);
        verify(bookingMapper).toDto(booking);
    }

    /**
     * Тест для endpoint: GET /bookings/user/{userId}
     * Назначение: Получение бронирований пользователя
     * Сценарий: Получение бронирований администратором
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Администратор имеет доступ к любым бронированиям
     * Бизнес-логика:
     * 1. Проверяет роль ADMIN у текущего пользователя
     * 2. Разрешает доступ к бронированиям любого пользователя
     * 3. Возвращает список бронирований
     */
    @Test
    void getUserBookings_ByAdmin_ShouldReturnAnyUserBookings() {
        // Arrange
        setupUserAuthentication("ROLE_ADMIN");
        List<Booking> bookings = Arrays.asList(booking);
        List<BookingDto> bookingDtos = Arrays.asList(bookingDto);

        when(bookingService.getUserBookings(USER_ID)).thenReturn(bookings);
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);

        // Act
        ResponseEntity<List<BookingDto>> response = bookingController.getUserBookings(USER_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).getUserBookings(USER_ID);
    }

    /**
     * Тест для endpoint: GET /bookings/my
     * Назначение: Получение бронирований текущего пользователя
     * Сценарий: Успешное получение собственных бронирований
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает только бронирования текущего пользователя
     * Бизнес-логика:
     * 1. Определяет текущего аутентифицированного пользователя
     * 2. Запрашивает бронирования по ID текущего пользователя
     * 3. Преобразует результат в DTO формат
     */
    @Test
    void getMyBookings_WithAuthenticatedUser_ShouldReturnUserBookings() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        List<Booking> bookings = Arrays.asList(booking);
        List<BookingDto> bookingDtos = Arrays.asList(bookingDto);

        when(bookingService.getCurrentUserBookings()).thenReturn(bookings);
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);

        // Act
        ResponseEntity<List<BookingDto>> response = bookingController.getMyBookings();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(bookingService).getCurrentUserBookings();
        verify(bookingMapper).toDto(booking);
    }

    /**
     * Тест для endpoint: DELETE /bookings/{id}
     * Назначение: Отмена бронирования
     * Сценарий: Успешная отмена собственного бронирования пользователем
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Тело ответа пустое
     * Бизнес-логика:
     * 1. Проверяет права доступа (пользователь или администратор)
     * 2. Вызывает сервис для отмены бронирования
     * 3. Возвращает успешный статус без тела
     */
    @Test
    void cancelBooking_WithValidId_ShouldReturnOk() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        // Создаем mock Booking для возврата из сервиса
        Booking cancelledBooking = new Booking();
        cancelledBooking.setId(BOOKING_ID);
        cancelledBooking.setStatus(BookingStatus.CANCELLED);

        when(bookingService.cancelBooking(BOOKING_ID)).thenReturn(cancelledBooking);

        // Act
        ResponseEntity<Void> response = bookingController.cancelBooking(BOOKING_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(bookingService).cancelBooking(BOOKING_ID);
    }

    /**
     * Тест для endpoint: DELETE /bookings/{id}
     * Назначение: Отмена бронирования
     * Сценарий: Отмена бронирования администратором
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Администратор может отменять любые бронирования
     * Бизнес-логика:
     * 1. Проверяет роль ADMIN у текущего пользователя
     * 2. Разрешает отмену любого бронирования
     * 3. Вызывает сервис отмены
     */
    @Test
    void cancelBooking_ByAdmin_ShouldCancelAnyBooking() {
        // Arrange
        setupUserAuthentication("ROLE_ADMIN");
        // Создаем mock Booking для возврата из сервиса
        Booking cancelledBooking = new Booking();
        cancelledBooking.setId(BOOKING_ID);
        cancelledBooking.setStatus(BookingStatus.CANCELLED);

        when(bookingService.cancelBooking(BOOKING_ID)).thenReturn(cancelledBooking);

        // Act
        ResponseEntity<Void> response = bookingController.cancelBooking(BOOKING_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).cancelBooking(BOOKING_ID);
    }

    /**
     * Тест для endpoint: GET /bookings
     * Назначение: Получение всех бронирований
     * Сценарий: Успешное получение списка всех бронирований администратором
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает полный список бронирований
     * Бизнес-логика:
     * 1. Проверяет роль ADMIN у текущего пользователя
     * 2. Получает все бронирования из сервиса
     * 3. Преобразует в DTO формат и возвращает
     */
    @Test
    void getAllBookings_ByAdmin_ShouldReturnAllBookings() {
        // Arrange
        setupUserAuthentication("ROLE_ADMIN");
        List<Booking> bookings = Arrays.asList(booking);
        List<BookingDto> bookingDtos = Arrays.asList(bookingDto);

        when(bookingService.getAllBookings()).thenReturn(bookings);
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);

        // Act
        ResponseEntity<List<BookingDto>> response = bookingController.getAllBookings();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(bookingService).getAllBookings();
        verify(bookingMapper).toDto(booking);
    }

    /**
     * Тест для endpoint: GET /bookings/recommendations
     * Назначение: Получение рекомендованных номеров
     * Сценарий: Успешное получение рекомендаций по датам
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает список рекомендованных номеров
     * Бизнес-логика:
     * 1. Валидирует параметры дат
     * 2. Запрашивает рекомендации из сервиса
     * 3. Возвращает список RoomRecommendation
     */
    @Test
    void getRecommendedRooms_WithValidDates_ShouldReturnRecommendations() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        RoomRecommendation recommendation = new RoomRecommendation();
        recommendation.setId(456L);
        recommendation.setType("DELUXE");
        recommendation.setPrice(200.0);
        recommendation.setTimesBooked(5);

        List<RoomRecommendation> recommendations = Arrays.asList(recommendation);

        when(bookingService.getRecommendedRooms(startDate, endDate)).thenReturn(recommendations);

        // Act
        ResponseEntity<List<RoomRecommendation>> response =
                bookingController.getRecommendedRooms(startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(456L, response.getBody().get(0).getId());

        verify(bookingService).getRecommendedRooms(startDate, endDate);
    }

    /**
     * Тест для endpoint: GET /bookings/recommendations
     * Назначение: Получение рекомендованных номеров
     * Сценарий: Пустой список рекомендаций
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает пустой список
     * Бизнес-логика:
     * 1. Обрабатывает случай отсутствия доступных номеров
     * 2. Возвращает корректный пустой ответ
     */
    @Test
    void getRecommendedRooms_WithNoRecommendations_ShouldReturnEmptyList() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        when(bookingService.getRecommendedRooms(startDate, endDate)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<RoomRecommendation>> response =
                bookingController.getRecommendedRooms(startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(bookingService).getRecommendedRooms(startDate, endDate);
    }

    /**
     * Тест для обработки исключений в контроллере
     * Назначение: Проверка обработки ошибок сервиса
     * Сценарий: Сервис бросает исключение при создании бронирования
     * Ожидаемый результат:
     * - Исключение пробрасывается из контроллера
     * - Глобальный обработчик перехватывает исключение
     */
    @Test
    void createBooking_WhenServiceThrowsException_ShouldPropagateException() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        when(bookingMapper.toEntity(bookingRequest)).thenReturn(booking);
        when(bookingService.createBooking(any(Booking.class), any(String.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingController.createBooking(bookingRequest));

        assertEquals("Service error", exception.getMessage());

        verify(bookingMapper).toEntity(bookingRequest);
        verify(bookingService).createBooking(any(Booking.class), any(String.class));
        verify(bookingMapper, never()).toDto(any(Booking.class));
    }

    private void setupUserAuthentication(String role) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "testUser",
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}