package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.dto.BookingDto;
import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.mapper.BookingMapper;
import com.hotelbooking.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

//    @Mock
//    private BookingService bookingService;
//
//    @Mock
//    private BookingMapper bookingMapper;
//
//    @InjectMocks
//    private BookingController bookingController;
//
//    private Booking createBooking(Long id, Long userId, Long roomId, LocalDate startDate, LocalDate endDate,
//                                  BookingStatus status, String correlationId) {
//        Booking booking = new Booking();
//        booking.setId(id);
//        booking.setUserId(userId);
//        booking.setRoomId(roomId);
//        booking.setStartDate(startDate);
//        booking.setEndDate(endDate);
//        booking.setStatus(status);
//        booking.setCorrelationId(correlationId);
//        booking.setCreatedAt(LocalDateTime.now());
//        booking.setUpdatedAt(LocalDateTime.now());
//        return booking;
//    }
//
//    private BookingDto createBookingDto(Long id, Long userId, String username, Long roomId, LocalDate startDate,
//                                        LocalDate endDate, BookingStatus status, String correlationId) {
//        BookingDto dto = new BookingDto();
//        dto.setId(id);
//        dto.setUserId(userId);
//        dto.setUsername(username);
//        dto.setRoomId(roomId);
//        dto.setStartDate(startDate);
//        dto.setEndDate(endDate);
//        dto.setStatus(status);
//        dto.setCorrelationId(correlationId);
//        dto.setCreatedAt(LocalDateTime.now());
//        dto.setUpdatedAt(LocalDateTime.now());
//        return dto;
//    }
//
//    private BookingRequest createBookingRequest(Long userId, Long roomId, LocalDate startDate, LocalDate endDate, String correlationId) {
//        BookingRequest request = new BookingRequest();
//        request.setUserId(userId);
//        request.setRoomId(roomId);
//        request.setStartDate(startDate);
//        request.setEndDate(endDate);
//        request.setCorrelationId(correlationId);
//        return request;
//    }
//
//    /**
//     * Тест для endpoint: POST /bookings
//     * Назначение: Создание бронирования с предоставленным correlationId
//     * Ожидаемый результат:
//     * - HTTP статус 200 (OK)
//     * - Возвращает созданное бронирование в формате DTO
//     * - Использует предоставленный correlationId
//     * - Вызывает сервис для создания бронирования
//     */
//    @Test
//    void createBooking_WithCorrelationId_ShouldCreateBooking() {
//        // Given
//        String correlationId = "test-correlation-123";
//        BookingRequest request = createBookingRequest(1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), correlationId);
//        Booking bookingEntity = createBooking(null, 1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, null);
//        Booking createdBooking = createBooking(1L, 1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, correlationId);
//        BookingDto expectedDto = createBookingDto(1L, 1L, "user1", 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, correlationId);
//
//        when(bookingMapper.toEntity(request)).thenReturn(bookingEntity);
//        when(bookingService.createBooking(bookingEntity, correlationId)).thenReturn(createdBooking);
//        when(bookingMapper.toDto(createdBooking)).thenReturn(expectedDto);
//
//        // When
//        ResponseEntity<BookingDto> response = bookingController.createBooking(request);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(expectedDto, response.getBody());
//        verify(bookingMapper, times(1)).toEntity(request);
//        verify(bookingService, times(1)).createBooking(bookingEntity, correlationId);
//        verify(bookingMapper, times(1)).toDto(createdBooking);
//    }
//
//    /**
//     * Тест для endpoint: POST /bookings
//     * Назначение: Создание бронирования без correlationId (генерируется автоматически)
//     * Ожидаемый результат:
//     * - HTTP статус 200 (OK)
//     * - Возвращает созданное бронирование в формате DTO
//     * - Генерирует новый correlationId
//     * - Вызывает сервис для создания бронирования
//     */
//    @Test
//    void createBooking_WithoutCorrelationId_ShouldGenerateCorrelationId() {
//        // Given
//        BookingRequest request = createBookingRequest(1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), null);
//        Booking bookingEntity = createBooking(null, 1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, null);
//        Booking createdBooking = createBooking(1L, 1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, "generated-uuid");
//        BookingDto expectedDto = createBookingDto(1L, 1L, "user1", 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, "generated-uuid");
//
//        when(bookingMapper.toEntity(request)).thenReturn(bookingEntity);
//        when(bookingService.createBooking(eq(bookingEntity), anyString())).thenReturn(createdBooking);
//        when(bookingMapper.toDto(createdBooking)).thenReturn(expectedDto);
//
//        // When
//        ResponseEntity<BookingDto> response = bookingController.createBooking(request);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(expectedDto, response.getBody());
//        verify(bookingMapper, times(1)).toEntity(request);
//        verify(bookingService, times(1)).createBooking(eq(bookingEntity), anyString());
//        verify(bookingMapper, times(1)).toDto(createdBooking);
//    }
//
//    /**
//     * Тест для endpoint: GET /bookings/user/{userId}
//     * Назначение: Получение бронирований пользователя по ID
//     * Ожидаемый результат:
//     * - HTTP статус 200 (OK)
//     * - Возвращает список бронирований пользователя в формате DTO
//     * - Вызывает сервис для получения бронирований пользователя
//     */
//    @Test
//    void getUserBookings_ShouldReturnUserBookings() {
//        // Given
//        Long userId = 1L;
//        Booking booking1 = createBooking(1L, userId, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, "corr-1");
//        Booking booking2 = createBooking(2L, userId, 102L,
//                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3), BookingStatus.CANCELLED, "corr-2");
//        List<Booking> bookings = Arrays.asList(booking1, booking2);
//
//        BookingDto dto1 = createBookingDto(1L, userId, "user1", 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, "corr-1");
//        BookingDto dto2 = createBookingDto(2L, userId, "user1", 102L,
//                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3), BookingStatus.CANCELLED, "corr-2");
//        List<BookingDto> expectedDtos = Arrays.asList(dto1, dto2);
//
//        when(bookingService.getUserBookings(userId)).thenReturn(bookings);
//        when(bookingMapper.toDto(booking1)).thenReturn(dto1);
//        when(bookingMapper.toDto(booking2)).thenReturn(dto2);
//
//        // When
//        ResponseEntity<List<BookingDto>> response = bookingController.getUserBookings(userId);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(2, response.getBody().size());
//        assertEquals(expectedDtos, response.getBody());
//        verify(bookingService, times(1)).getUserBookings(userId);
//        verify(bookingMapper, times(2)).toDto(any(Booking.class));
//    }
//
//    /**
//     * Тест для endpoint: GET /bookings/my
//     * Назначение: Получение бронирований текущего пользователя
//     * Ожидаемый результат:
//     * - HTTP статус 200 (OK)
//     * - Возвращает список бронирований текущего пользователя в формате DTO
//     * - Вызывает сервис для получения бронирований текущего пользователя
//     */
//    @Test
//    void getMyBookings_ShouldReturnCurrentUserBookings() {
//        // Given
//        Booking booking1 = createBooking(1L, 1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, "corr-1");
//        Booking booking2 = createBooking(2L, 1L, 102L,
//                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3), BookingStatus.PENDING, "corr-2");
//        List<Booking> bookings = Arrays.asList(booking1, booking2);
//
//        BookingDto dto1 = createBookingDto(1L, 1L, "currentuser", 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, "corr-1");
//        BookingDto dto2 = createBookingDto(2L, 1L, "currentuser", 102L,
//                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3), BookingStatus.PENDING, "corr-2");
//        List<BookingDto> expectedDtos = Arrays.asList(dto1, dto2);
//
//        when(bookingService.getCurrentUserBookings()).thenReturn(bookings);
//        when(bookingMapper.toDto(booking1)).thenReturn(dto1);
//        when(bookingMapper.toDto(booking2)).thenReturn(dto2);
//
//        // When
//        ResponseEntity<List<BookingDto>> response = bookingController.getMyBookings();
//
//        // Then
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(2, response.getBody().size());
//        assertEquals(expectedDtos, response.getBody());
//        verify(bookingService, times(1)).getCurrentUserBookings();
//        verify(bookingMapper, times(2)).toDto(any(Booking.class));
//    }
//
//    /**
//     * Тест для endpoint: DELETE /bookings/{id}
//     * Назначение: Отмена бронирования по ID
//     * Ожидаемый результат:
//     * - HTTP статус 200 (OK)
//     * - Тело ответа пустое
//     * - Вызывает сервис для отмены бронирования
//     */
//    @Test
//    void cancelBooking_ShouldCancelBookingAndReturnOk() {
//        // Given
//        Long bookingId = 1L;
//        doNothing().when(bookingService).cancelBooking(bookingId);
//
//        // When
//        ResponseEntity<Void> response = bookingController.cancelBooking(bookingId);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNull(response.getBody());
//        verify(bookingService, times(1)).cancelBooking(bookingId);
//    }
//
//    /**
//     * Тест для endpoint: GET /bookings
//     * Назначение: Получение всех бронирований (только для ADMIN)
//     * Ожидаемый результат:
//     * - HTTP статус 200 (OK)
//     * - Возвращает список всех бронирований в формате DTO
//     * - Вызывает сервис для получения всех бронирований
//     */
//    @Test
//    void getAllBookings_ShouldReturnAllBookings() {
//        // Given
//        Booking booking1 = createBooking(1L, 1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, "corr-1");
//        Booking booking2 = createBooking(2L, 2L, 102L,
//                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3), BookingStatus.CANCELLED, "corr-2");
//        Booking booking3 = createBooking(3L, 3L, 103L,
//                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 7), BookingStatus.PENDING, "corr-3");
//        List<Booking> bookings = Arrays.asList(booking1, booking2, booking3);
//
//        BookingDto dto1 = createBookingDto(1L, 1L, "user1", 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, "corr-1");
//        BookingDto dto2 = createBookingDto(2L, 2L, "user2", 102L,
//                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3), BookingStatus.CANCELLED, "corr-2");
//        BookingDto dto3 = createBookingDto(3L, 3L, "user3", 103L,
//                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 7), BookingStatus.PENDING, "corr-3");
//        List<BookingDto> expectedDtos = Arrays.asList(dto1, dto2, dto3);
//
//        when(bookingService.getAllBookings()).thenReturn(bookings);
//        when(bookingMapper.toDto(booking1)).thenReturn(dto1);
//        when(bookingMapper.toDto(booking2)).thenReturn(dto2);
//        when(bookingMapper.toDto(booking3)).thenReturn(dto3);
//
//        // When
//        ResponseEntity<List<BookingDto>> response = bookingController.getAllBookings();
//
//        // Then
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(3, response.getBody().size());
//        assertEquals(expectedDtos, response.getBody());
//        verify(bookingService, times(1)).getAllBookings();
//        verify(bookingMapper, times(3)).toDto(any(Booking.class));
//    }
//
//    /**
//     * Тест для endpoint: GET /bookings/user/{userId} (пустой список)
//     * Назначение: Получение пустого списка бронирований пользователя
//     * Ожидаемый результат:
//     * - HTTP статус 200 (OK)
//     * - Возвращает пустой список
//     * - Корректно обрабатывает отсутствие бронирований
//     */
//    @Test
//    void getUserBookings_WhenNoBookings_ShouldReturnEmptyList() {
//        // Given
//        Long userId = 1L;
//        List<Booking> emptyBookings = List.of();
//
//        when(bookingService.getUserBookings(userId)).thenReturn(emptyBookings);
//
//        // When
//        ResponseEntity<List<BookingDto>> response = bookingController.getUserBookings(userId);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().isEmpty());
//        verify(bookingService, times(1)).getUserBookings(userId);
//        verify(bookingMapper, never()).toDto(any(Booking.class));
//    }
//
//    /**
//     * Тест для endpoint: GET /bookings/my (пустой список)
//     * Назначение: Получение пустого списка бронирований текущего пользователя
//     * Ожидаемый результат:
//     * - HTTP статус 200 (OK)
//     * - Возвращает пустой список
//     * - Корректно обрабатывает отсутствие бронирований
//     */
//    @Test
//    void getMyBookings_WhenNoBookings_ShouldReturnEmptyList() {
//        // Given
//        List<Booking> emptyBookings = List.of();
//
//        when(bookingService.getCurrentUserBookings()).thenReturn(emptyBookings);
//
//        // When
//        ResponseEntity<List<BookingDto>> response = bookingController.getMyBookings();
//
//        // Then
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().isEmpty());
//        verify(bookingService, times(1)).getCurrentUserBookings();
//        verify(bookingMapper, never()).toDto(any(Booking.class));
//    }
//
//    /**
//     * Тест для endpoint: GET /bookings (пустой список)
//     * Назначение: Получение пустого списка всех бронирований
//     * Ожидаемый результат:
//     * - HTTP статус 200 (OK)
//     * - Возвращает пустой список
//     * - Корректно обрабатывает отсутствие бронирований
//     */
//    @Test
//    void getAllBookings_WhenNoBookings_ShouldReturnEmptyList() {
//        // Given
//        List<Booking> emptyBookings = List.of();
//
//        when(bookingService.getAllBookings()).thenReturn(emptyBookings);
//
//        // When
//        ResponseEntity<List<BookingDto>> response = bookingController.getAllBookings();
//
//        // Then
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().isEmpty());
//        verify(bookingService, times(1)).getAllBookings();
//        verify(bookingMapper, never()).toDto(any(Booking.class));
//    }
//
//    /**
//     * Тест для endpoint: POST /bookings с разными статусами бронирования
//     * Назначение: Создание бронирования с различными статусами
//     * Ожидаемый результат:
//     * - HTTP статус 200 (OK)
//     * - Возвращает созданное бронирование
//     * - Корректно обрабатывает различные сценарии бронирования
//     */
//    @Test
//    void createBooking_WithDifferentStatuses_ShouldCreateBooking() {
//        // Given
//        BookingRequest request = createBookingRequest(1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), "test-correlation");
//        Booking bookingEntity = createBooking(null, 1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.PENDING, null);
//        Booking createdBooking = createBooking(1L, 1L, 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, "test-correlation");
//        BookingDto expectedDto = createBookingDto(1L, 1L, "user1", 101L,
//                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), BookingStatus.CONFIRMED, "test-correlation");
//
//        when(bookingMapper.toEntity(request)).thenReturn(bookingEntity);
//        when(bookingService.createBooking(bookingEntity, "test-correlation")).thenReturn(createdBooking);
//        when(bookingMapper.toDto(createdBooking)).thenReturn(expectedDto);
//
//        // When
//        ResponseEntity<BookingDto> response = bookingController.createBooking(request);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals(expectedDto, response.getBody());
//        verify(bookingMapper, times(1)).toEntity(request);
//        verify(bookingService, times(1)).createBooking(bookingEntity, "test-correlation");
//        verify(bookingMapper, times(1)).toDto(createdBooking);
//    }
//
//    /**
//     * Тест для endpoint: DELETE /bookings/{id} с несуществующим ID
//     * Назначение: Попытка отмены несуществующего бронирования
//     * Ожидаемый результат:
//     * - Выбрасывает исключение из сервиса (тестируется интеграция)
//     * - Вызывает сервис для отмены бронирования
//     */
//    @Test
//    void cancelBooking_WithNonExistingId_ShouldPropagateException() {
//        // Given
//        Long bookingId = 999L;
//        doThrow(new RuntimeException("Booking not found")).when(bookingService).cancelBooking(bookingId);
//
//        // When & Then
//        assertThrows(RuntimeException.class,
//                () -> bookingController.cancelBooking(bookingId));
//
//        verify(bookingService, times(1)).cancelBooking(bookingId);
//    }
}