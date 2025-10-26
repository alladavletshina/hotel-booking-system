package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.RoomDto;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.mapper.RoomMapper;
import com.hotelbooking.hotel.service.RoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private RoomService roomService;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomController roomController;

    private Room createRoom(Long id, String number, String type, Double price, boolean available, Integer timesBooked, Long hotelId) {
        Room room = new Room();
        room.setId(id);
        room.setNumber(number);
        room.setType(type);
        room.setPrice(price);
        room.setAvailable(available);
        room.setTimesBooked(timesBooked);
        return room;
    }

    private RoomDto createRoomDto(Long id, String number, String type, Double price, Boolean available, Integer timesBooked, Long hotelId) {
        return new RoomDto(id, number, type, price, available, timesBooked, hotelId);
    }

    /**
     * Тест для endpoint: GET /rooms
     * Назначение: Получение списка всех доступных номеров
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает список доступных номеров в формате DTO
     * - Вызывает сервис для получения доступных номеров
     */
    @Test
    void getAvailableRooms_ShouldReturnListOfAvailableRooms() {
        // Given
        Room room1 = createRoom(1L, "101", "STANDARD", 100.0, true, 5, 1L);
        Room room2 = createRoom(2L, "102", "DELUXE", 200.0, true, 10, 1L);
        List<Room> rooms = Arrays.asList(room1, room2);

        RoomDto roomDto1 = createRoomDto(1L, "101", "STANDARD", 100.0, true, 5, 1L);
        RoomDto roomDto2 = createRoomDto(2L, "102", "DELUXE", 200.0, true, 10, 1L);
        List<RoomDto> expectedDtos = Arrays.asList(roomDto1, roomDto2);

        when(roomService.findAvailableRooms()).thenReturn(rooms);
        when(roomMapper.toDto(room1)).thenReturn(roomDto1);
        when(roomMapper.toDto(room2)).thenReturn(roomDto2);

        // When
        ResponseEntity<List<RoomDto>> response = roomController.getAvailableRooms();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(expectedDtos, response.getBody());
        verify(roomService, times(1)).findAvailableRooms();
        verify(roomMapper, times(2)).toDto(any(Room.class));
    }

    /**
     * Тест для endpoint: GET /rooms/{id}
     * Назначение: Получение номера по ID
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает номер в формате DTO
     * - Вызывает сервис для поиска номера по ID
     */
    @Test
    void getRoom_WithExistingId_ShouldReturnRoom() {
        // Given
        Long roomId = 1L;
        Room room = createRoom(roomId, "101", "STANDARD", 100.0, true, 5, 1L);
        RoomDto expectedDto = createRoomDto(roomId, "101", "STANDARD", 100.0, true, 5, 1L);

        when(roomService.findById(roomId)).thenReturn(room);
        when(roomMapper.toDto(room)).thenReturn(expectedDto);

        // When
        ResponseEntity<RoomDto> response = roomController.getRoom(roomId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedDto, response.getBody());
        verify(roomService, times(1)).findById(roomId);
        verify(roomMapper, times(1)).toDto(room);
    }

    /**
     * Тест для endpoint: GET /rooms/hotel/{hotelId}
     * Назначение: Получение всех номеров по ID отеля
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает список номеров отеля в формате DTO
     * - Вызывает сервис для поиска номеров по ID отеля
     */
    @Test
    void getRoomsByHotel_ShouldReturnRoomsList() {
        // Given
        Long hotelId = 1L;
        Room room1 = createRoom(1L, "101", "STANDARD", 100.0, true, 5, hotelId);
        Room room2 = createRoom(2L, "102", "DELUXE", 200.0, false, 8, hotelId);
        List<Room> rooms = Arrays.asList(room1, room2);

        RoomDto roomDto1 = createRoomDto(1L, "101", "STANDARD", 100.0, true, 5, hotelId);
        RoomDto roomDto2 = createRoomDto(2L, "102", "DELUXE", 200.0, false, 8, hotelId);
        List<RoomDto> expectedDtos = Arrays.asList(roomDto1, roomDto2);

        when(roomService.findRoomsByHotelId(hotelId)).thenReturn(rooms);
        when(roomMapper.toDto(room1)).thenReturn(roomDto1);
        when(roomMapper.toDto(room2)).thenReturn(roomDto2);

        // When
        ResponseEntity<List<RoomDto>> response = roomController.getRoomsByHotel(hotelId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(expectedDtos, response.getBody());
        verify(roomService, times(1)).findRoomsByHotelId(hotelId);
        verify(roomMapper, times(2)).toDto(any(Room.class));
    }

    /**
     * Тест для endpoint: DELETE /rooms/{id}
     * Назначение: Удаление номера по ID
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Тело ответа пустое
     * - Вызывает сервис для удаления номера
     */
    @Test
    void deleteRoom_ShouldDeleteRoomAndReturnOk() {
        // Given
        Long roomId = 1L;
        doNothing().when(roomService).deleteById(roomId);

        // When
        ResponseEntity<Void> response = roomController.deleteRoom(roomId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(roomService, times(1)).deleteById(roomId);
    }

    /**
     * Тест для endpoint: GET /rooms/recommend
     * Назначение: Получение рекомендованных номеров
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает список рекомендованных номеров в формате DTO
     * - Вызывает сервис для получения рекомендованных номеров
     */
    @Test
    void getRecommendedRooms_ShouldReturnRecommendedRooms() {
        // Given
        Room room1 = createRoom(1L, "101", "STANDARD", 100.0, true, 15, 1L);
        Room room2 = createRoom(2L, "201", "SUITE", 300.0, true, 20, 1L);
        List<Room> rooms = Arrays.asList(room1, room2);

        RoomDto roomDto1 = createRoomDto(1L, "101", "STANDARD", 100.0, true, 15, 1L);
        RoomDto roomDto2 = createRoomDto(2L, "201", "SUITE", 300.0, true, 20, 1L);
        List<RoomDto> expectedDtos = Arrays.asList(roomDto1, roomDto2);

        when(roomService.findRecommendedRooms()).thenReturn(rooms);
        when(roomMapper.toDto(room1)).thenReturn(roomDto1);
        when(roomMapper.toDto(room2)).thenReturn(roomDto2);

        // When
        ResponseEntity<List<RoomDto>> response = roomController.getRecommendedRooms();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(expectedDtos, response.getBody());
        verify(roomService, times(1)).findRecommendedRooms();
        verify(roomMapper, times(2)).toDto(any(Room.class));
    }

    /**
     * Тест для endpoint: POST /rooms
     * Назначение: Создание нового номера
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает созданный номер в формате DTO
     * - Вызывает маппер и сервис для сохранения номера
     */
    @Test
    void createRoom_ShouldCreateAndReturnRoom() {
        // Given
        RoomDto inputDto = createRoomDto(null, "101", "STANDARD", 100.0, true, 0, 1L);
        Room roomToSave = createRoom(null, "101", "STANDARD", 100.0, true, 0, 1L);
        Room savedRoom = createRoom(1L, "101", "STANDARD", 100.0, true, 0, 1L);
        RoomDto expectedDto = createRoomDto(1L, "101", "STANDARD", 100.0, true, 0, 1L);

        when(roomMapper.toEntity(inputDto)).thenReturn(roomToSave);
        when(roomService.save(roomToSave)).thenReturn(savedRoom);
        when(roomMapper.toDto(savedRoom)).thenReturn(expectedDto);

        // When
        ResponseEntity<RoomDto> response = roomController.createRoom(inputDto);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedDto, response.getBody());
        verify(roomMapper, times(1)).toEntity(inputDto);
        verify(roomService, times(1)).save(roomToSave);
        verify(roomMapper, times(1)).toDto(savedRoom);
    }

    /**
     * Тест для endpoint: POST /rooms/{id}/confirm-availability
     * Назначение: Подтверждение доступности номера
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает boolean значение доступности
     * - Вызывает сервис для подтверждения доступности
     */
    @Test
    void confirmAvailability_ShouldReturnAvailabilityStatus() {
        // Given
        Long roomId = 1L;
        boolean expectedAvailability = true;

        when(roomService.confirmAvailability(roomId)).thenReturn(expectedAvailability);

        // When
        ResponseEntity<Boolean> response = roomController.confirmAvailability(roomId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedAvailability, response.getBody());
        verify(roomService, times(1)).confirmAvailability(roomId);
    }

    /**
     * Тест для endpoint: POST /rooms/{id}/release
     * Назначение: Снятие временной блокировки номера
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Тело ответа пустое
     * - Вызывает сервис для снятия блокировки
     */
    @Test
    void releaseRoom_ShouldReleaseRoomAndReturnOk() {
        // Given
        Long roomId = 1L;
        doNothing().when(roomService).releaseRoom(roomId);

        // When
        ResponseEntity<Void> response = roomController.releaseRoom(roomId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(roomService, times(1)).releaseRoom(roomId);
    }

    /**
     * Тест для endpoint: GET /rooms (пустой список)
     * Назначение: Получение пустого списка доступных номеров
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает пустой список
     * - Корректно обрабатывает отсутствие доступных номеров
     */
    @Test
    void getAvailableRooms_WhenNoRoomsAvailable_ShouldReturnEmptyList() {
        // Given
        List<Room> emptyRooms = List.of();
        when(roomService.findAvailableRooms()).thenReturn(emptyRooms);

        // When
        ResponseEntity<List<RoomDto>> response = roomController.getAvailableRooms();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(roomService, times(1)).findAvailableRooms();
        verify(roomMapper, never()).toDto(any(Room.class));
    }

    /**
     * Тест для endpoint: GET /rooms/hotel/{hotelId} (пустой список)
     * Назначение: Получение пустого списка номеров отеля
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает пустой список
     * - Корректно обрабатывает отсутствие номеров в отеле
     */
    @Test
    void getRoomsByHotel_WhenNoRoomsInHotel_ShouldReturnEmptyList() {
        // Given
        Long hotelId = 1L;
        List<Room> emptyRooms = List.of();
        when(roomService.findRoomsByHotelId(hotelId)).thenReturn(emptyRooms);

        // When
        ResponseEntity<List<RoomDto>> response = roomController.getRoomsByHotel(hotelId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(roomService, times(1)).findRoomsByHotelId(hotelId);
        verify(roomMapper, never()).toDto(any(Room.class));
    }

    /**
     * Тест для endpoint: GET /rooms/recommend (пустой список)
     * Назначение: Получение пустого списка рекомендованных номеров
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает пустой список
     * - Корректно обрабатывает отсутствие рекомендованных номеров
     */
    @Test
    void getRecommendedRooms_WhenNoRecommendedRooms_ShouldReturnEmptyList() {
        // Given
        List<Room> emptyRooms = List.of();
        when(roomService.findRecommendedRooms()).thenReturn(emptyRooms);

        // When
        ResponseEntity<List<RoomDto>> response = roomController.getRecommendedRooms();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(roomService, times(1)).findRecommendedRooms();
        verify(roomMapper, never()).toDto(any(Room.class));
    }

    /**
     * Тест для endpoint: POST /rooms/{id}/confirm-availability (недоступный номер)
     * Назначение: Подтверждение недоступности номера
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает false
     * - Корректно обрабатывает недоступный номер
     */
    @Test
    void confirmAvailability_WhenRoomNotAvailable_ShouldReturnFalse() {
        // Given
        Long roomId = 1L;
        boolean expectedAvailability = false;

        when(roomService.confirmAvailability(roomId)).thenReturn(expectedAvailability);

        // When
        ResponseEntity<Boolean> response = roomController.confirmAvailability(roomId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedAvailability, response.getBody());
        verify(roomService, times(1)).confirmAvailability(roomId);
    }
}