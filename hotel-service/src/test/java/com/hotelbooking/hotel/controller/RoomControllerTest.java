package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.*;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.mapper.RoomMapper;
import com.hotelbooking.hotel.service.RoomService;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private RoomService roomService;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomController roomController;

    private Room testRoom;
    private RoomDto testRoomDto;
    private Hotel testHotel;
    private final Long ROOM_ID = 1L;
    private final Long HOTEL_ID = 1L;
    private final LocalDate START_DATE = LocalDate.now().plusDays(1);
    private final LocalDate END_DATE = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        // Создаем тестовый отель
        testHotel = new Hotel();
        testHotel.setId(HOTEL_ID);
        testHotel.setName("Test Hotel");
        testHotel.setAddress("Test Address");

        // Создаем тестовый номер в соответствии с структурой entity
        testRoom = new Room();
        testRoom.setId(ROOM_ID);
        testRoom.setNumber("101");
        testRoom.setType("DELUXE");
        testRoom.setPrice(200.0);
        testRoom.setAvailable(true);
        testRoom.setTimesBooked(5);
        testRoom.setHotel(testHotel); // Устанавливаем объект Hotel, а не hotelId

        // Создаем тестовый DTO
        testRoomDto = new RoomDto();
        testRoomDto.setId(ROOM_ID);
        testRoomDto.setHotelId(HOTEL_ID); // В DTO может быть прямой ID
        testRoomDto.setNumber("101");
        testRoomDto.setType("DELUXE");
        testRoomDto.setPrice(200.0);
        testRoomDto.setAvailable(true);
        testRoomDto.setTimesBooked(5);
    }

    /**
     * Тест для endpoint: GET /rooms
     * Назначение: Получение всех доступных номеров
     * Сценарий: Успешное получение списка доступных номеров
     */
    @Test
    void getAvailableRooms_ShouldReturnRoomsList() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        List<Room> rooms = Arrays.asList(testRoom);
        List<RoomDto> roomDtos = Arrays.asList(testRoomDto);

        when(roomService.findAvailableRooms()).thenReturn(rooms);
        when(roomMapper.toDto(testRoom)).thenReturn(testRoomDto);

        // Act
        ResponseEntity<List<RoomDto>> response = roomController.getAvailableRooms();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(ROOM_ID, response.getBody().get(0).getId());

        verify(roomService).findAvailableRooms();
        verify(roomMapper).toDto(testRoom);
    }

    /**
     * Тест для endpoint: GET /rooms/{id}
     * Назначение: Получение номера по ID
     * Сценарий: Успешное получение существующего номера
     */
    @Test
    void getRoom_WithExistingId_ShouldReturnRoom() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        when(roomService.findById(ROOM_ID)).thenReturn(testRoom);
        when(roomMapper.toDto(testRoom)).thenReturn(testRoomDto);

        // Act
        ResponseEntity<RoomDto> response = roomController.getRoom(ROOM_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ROOM_ID, response.getBody().getId());

        verify(roomService).findById(ROOM_ID);
        verify(roomMapper).toDto(testRoom);
    }

    /**
     * Тест для endpoint: GET /rooms/hotel/{hotelId}
     * Назначение: Получение номеров по отелю
     * Сценарий: Успешное получение номеров отеля
     */
    @Test
    void getRoomsByHotel_WithExistingHotel_ShouldReturnRoomsList() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        List<Room> rooms = Arrays.asList(testRoom);
        List<RoomDto> roomDtos = Arrays.asList(testRoomDto);

        when(roomService.findRoomsByHotelId(HOTEL_ID)).thenReturn(rooms);
        when(roomMapper.toDto(testRoom)).thenReturn(testRoomDto);

        // Act
        ResponseEntity<List<RoomDto>> response = roomController.getRoomsByHotel(HOTEL_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(HOTEL_ID, response.getBody().get(0).getHotelId());

        verify(roomService).findRoomsByHotelId(HOTEL_ID);
        verify(roomMapper).toDto(testRoom);
    }

    /**
     * Тест для endpoint: DELETE /rooms/{id}
     * Назначение: Удаление номера по ID
     * Сценарий: Успешное удаление номера администратором
     */
    @Test
    void deleteRoom_WithAdminRole_ShouldDeleteRoom() {
        // Arrange
        setupUserAuthentication("ROLE_ADMIN");
        doNothing().when(roomService).deleteById(ROOM_ID);

        // Act
        ResponseEntity<Void> response = roomController.deleteRoom(ROOM_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(roomService).deleteById(ROOM_ID);
    }

    /**
     * Тест для endpoint: GET /rooms/recommend
     * Назначение: Получение рекомендованных номеров
     * Сценарий: Успешное получение рекомендованных номеров
     */
    @Test
    void getRecommendedRooms_ShouldReturnRecommendedRooms() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        List<Room> rooms = Arrays.asList(testRoom);
        List<RoomDto> roomDtos = Arrays.asList(testRoomDto);

        when(roomService.findRecommendedRooms()).thenReturn(rooms);
        when(roomMapper.toDto(testRoom)).thenReturn(testRoomDto);

        // Act
        ResponseEntity<List<RoomDto>> response = roomController.getRecommendedRooms();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(roomService).findRecommendedRooms();
        verify(roomMapper).toDto(testRoom);
    }

    /**
     * Тест для endpoint: POST /rooms
     * Назначение: Создание нового номера
     * Сценарий: Успешное создание номера администратором
     */
    @Test
    void createRoom_WithAdminRole_ShouldCreateRoom() {
        // Arrange
        setupUserAuthentication("ROLE_ADMIN");
        when(roomMapper.toEntity(testRoomDto)).thenReturn(testRoom);
        when(roomService.save(testRoom)).thenReturn(testRoom);
        when(roomMapper.toDto(testRoom)).thenReturn(testRoomDto);

        // Act
        ResponseEntity<RoomDto> response = roomController.createRoom(testRoomDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ROOM_ID, response.getBody().getId());

        verify(roomMapper).toEntity(testRoomDto);
        verify(roomService).save(testRoom);
        verify(roomMapper).toDto(testRoom);
    }

    /**
     * Тест для endpoint: GET /rooms/{id}/availability
     * Назначение: Проверка доступности номера на даты
     * Сценарий: Номер доступен на указанные даты
     */
    @Test
    void checkAvailability_WithAvailableRoom_ShouldReturnTrue() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        when(roomService.isRoomAvailable(ROOM_ID, START_DATE, END_DATE)).thenReturn(true);

        // Act
        ResponseEntity<Boolean> response = roomController.checkAvailability(ROOM_ID, START_DATE, END_DATE);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());

        verify(roomService).isRoomAvailable(ROOM_ID, START_DATE, END_DATE);
    }

    /**
     * Тест для endpoint: GET /rooms/{id}/availability
     * Назначение: Проверка доступности номера на даты
     * Сценарий: Номер недоступен на указанные даты
     */
    @Test
    void checkAvailability_WithUnavailableRoom_ShouldReturnFalse() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        when(roomService.isRoomAvailable(ROOM_ID, START_DATE, END_DATE)).thenReturn(false);

        // Act
        ResponseEntity<Boolean> response = roomController.checkAvailability(ROOM_ID, START_DATE, END_DATE);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody());

        verify(roomService).isRoomAvailable(ROOM_ID, START_DATE, END_DATE);
    }

    /**
     * Тест для endpoint: GET /rooms/available
     * Назначение: Поиск доступных номеров на даты
     * Сценарий: Успешное получение доступных номеров
     */
    @Test
    void getAvailableRoomsForDates_ShouldReturnAvailableRooms() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        List<Room> rooms = Arrays.asList(testRoom);
        List<RoomDto> roomDtos = Arrays.asList(testRoomDto);

        when(roomService.findAvailableRooms(START_DATE, END_DATE)).thenReturn(rooms);
        when(roomMapper.toDto(testRoom)).thenReturn(testRoomDto);

        // Act
        ResponseEntity<List<RoomDto>> response = roomController.getAvailableRoomsForDates(START_DATE, END_DATE);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(roomService).findAvailableRooms(START_DATE, END_DATE);
        verify(roomMapper).toDto(testRoom);
    }

    /**
     * Тест для endpoint: GET /rooms/recommend/date
     * Назначение: Получение рекомендованных номеров на даты
     * Сценарий: Успешное получение рекомендованных номеров
     */
    @Test
    void getRecommendedRoomsForDates_ShouldReturnRecommendedRooms() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        List<Room> rooms = Arrays.asList(testRoom);
        List<RoomDto> roomDtos = Arrays.asList(testRoomDto);

        when(roomService.findRecommendedRooms(START_DATE, END_DATE)).thenReturn(rooms);
        when(roomMapper.toDto(testRoom)).thenReturn(testRoomDto);

        // Act
        ResponseEntity<List<RoomDto>> response = roomController.getRecommendedRoomsForDates(START_DATE, END_DATE);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(roomService).findRecommendedRooms(START_DATE, END_DATE);
        verify(roomMapper).toDto(testRoom);
    }

    /**
     * Тест для endpoint: POST /rooms/{id}/confirm-availability-with-dates
     * Назначение: Подтверждение доступности с временной блокировкой
     * Сценарий: Успешное подтверждение доступности
     */
    @Test
    void confirmAvailabilityWithDates_WithAvailableRoom_ShouldReturnTrue() {
        // Arrange
        setupUserAuthentication("ROLE_INTERNAL");
        Long bookingId = 100L;
        AvailabilityRequest request = new AvailabilityRequest();
        request.setStartDate(START_DATE);
        request.setEndDate(END_DATE);
        request.setBookingId(bookingId);

        when(roomService.confirmAvailability(ROOM_ID, START_DATE, END_DATE, bookingId)).thenReturn(true);

        // Act
        ResponseEntity<Boolean> response = roomController.confirmAvailabilityWithDates(ROOM_ID, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());

        verify(roomService).confirmAvailability(ROOM_ID, START_DATE, END_DATE, bookingId);
    }

    /**
     * Тест для endpoint: POST /rooms/{id}/confirm-availability
     * Назначение: Подтверждение доступности (устаревшая версия)
     * Сценарий: Успешное подтверждение доступности
     */
    @Test
    void confirmAvailability_ShouldCallService() {
        // Arrange
        setupUserAuthentication("ROLE_INTERNAL");
        when(roomService.confirmAvailability(ROOM_ID)).thenReturn(true);

        // Act
        ResponseEntity<Boolean> response = roomController.confirmAvailability(ROOM_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(roomService).confirmAvailability(ROOM_ID);
    }

    /**
     * Тест для endpoint: POST /rooms/{id}/release-booking
     * Назначение: Снятие блокировки по бронированию
     * Сценарий: Успешное снятие блокировки
     */
    @Test
    void releaseRoomWithBooking_ShouldReleaseRoom() {
        // Arrange
        setupUserAuthentication("ROLE_INTERNAL");
        Long bookingId = 100L;
        ReleaseRequest request = new ReleaseRequest();
        request.setBookingId(bookingId);

        doNothing().when(roomService).releaseRoom(ROOM_ID, bookingId);

        // Act
        ResponseEntity<Void> response = roomController.releaseRoomWithBooking(ROOM_ID, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(roomService).releaseRoom(ROOM_ID, bookingId);
    }

    /**
     * Тест для endpoint: POST /rooms/{id}/release
     * Назначение: Снятие блокировки (устаревшая версия)
     * Сценарий: Успешное снятие блокировки
     */
    @Test
    void releaseRoom_ShouldCallService() {
        // Arrange
        setupUserAuthentication("ROLE_INTERNAL");
        doNothing().when(roomService).releaseRoom(ROOM_ID);

        // Act
        ResponseEntity<Void> response = roomController.releaseRoom(ROOM_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(roomService).releaseRoom(ROOM_ID);
    }

    /**
     * Тест для endpoint: POST /rooms/{id}/confirm-booking
     * Назначение: Подтверждение бронирования
     * Сценарий: Успешное подтверждение бронирования
     */
    @Test
    void confirmBooking_ShouldConfirmBooking() {
        // Arrange
        setupUserAuthentication("ROLE_INTERNAL");
        Long bookingId = 100L;
        BookingConfirmationRequest request = new BookingConfirmationRequest();
        request.setBookingId(bookingId);

        doNothing().when(roomService).confirmBooking(ROOM_ID, bookingId);

        // Act
        ResponseEntity<Void> response = roomController.confirmBooking(ROOM_ID, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(roomService).confirmBooking(ROOM_ID, bookingId);
    }

    /**
     * Тест для endpoint: POST /rooms/{id}/cancel-booking
     * Назначение: Отмена бронирования
     * Сценарий: Успешная отмена бронирования
     */
    @Test
    void cancelBooking_ShouldCancelBooking() {
        // Arrange
        setupUserAuthentication("ROLE_INTERNAL");
        Long bookingId = 100L;
        BookingConfirmationRequest request = new BookingConfirmationRequest();
        request.setBookingId(bookingId);

        doNothing().when(roomService).cancelBooking(ROOM_ID, bookingId);

        // Act
        ResponseEntity<Void> response = roomController.cancelBooking(ROOM_ID, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(roomService).cancelBooking(ROOM_ID, bookingId);
    }

    /**
     * Тест для endpoint: GET /rooms
     * Назначение: Получение всех доступных номеров
     * Сценарий: Пустой список доступных номеров
     */
    @Test
    void getAvailableRooms_WithEmptyList_ShouldReturnEmptyList() {
        // Arrange
        setupUserAuthentication("ROLE_USER");
        when(roomService.findAvailableRooms()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<RoomDto>> response = roomController.getAvailableRooms();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(roomService).findAvailableRooms();
        verify(roomMapper, never()).toDto(any(Room.class));
    }

    /**
     * Тест для endpoint: POST /rooms
     * Назначение: Создание нового номера
     * Сценарий: Создание номера с минимальными данными
     */
    @Test
    void createRoom_WithMinimalData_ShouldCreateRoom() {
        // Arrange
        setupUserAuthentication("ROLE_ADMIN");

        // Создаем минимальный DTO
        RoomDto minimalDto = new RoomDto();
        minimalDto.setHotelId(HOTEL_ID);
        minimalDto.setNumber("102");
        minimalDto.setType("STANDARD");
        minimalDto.setPrice(100.0);

        // Создаем минимальную сущность Room (маппер должен создать объект Hotel)
        Room minimalRoom = new Room();
        minimalRoom.setNumber("102");
        minimalRoom.setType("STANDARD");
        minimalRoom.setPrice(100.0);

        // Создаем сохраненную сущность
        Room savedRoom = new Room();
        savedRoom.setId(2L);
        savedRoom.setNumber("102");
        savedRoom.setType("STANDARD");
        savedRoom.setPrice(100.0);
        savedRoom.setHotel(testHotel); // Здесь уже установлен отель

        RoomDto savedDto = new RoomDto();
        savedDto.setId(2L);
        savedDto.setHotelId(HOTEL_ID);
        savedDto.setNumber("102");
        savedDto.setType("STANDARD");
        savedDto.setPrice(100.0);

        when(roomMapper.toEntity(minimalDto)).thenReturn(minimalRoom);
        when(roomService.save(minimalRoom)).thenReturn(savedRoom);
        when(roomMapper.toDto(savedRoom)).thenReturn(savedDto);

        // Act
        ResponseEntity<RoomDto> response = roomController.createRoom(minimalDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2L, response.getBody().getId());
        assertEquals("STANDARD", response.getBody().getType());

        verify(roomMapper).toEntity(minimalDto);
        verify(roomService).save(minimalRoom);
        verify(roomMapper).toDto(savedRoom);
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