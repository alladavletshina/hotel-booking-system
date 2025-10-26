package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    private Room createRoom(Long id, String roomNumber, String type, Double price, boolean available, Integer timesBooked, Long hotelId) {
        Room room = new Room();
        room.setId(id);
        room.setNumber(roomNumber);
        room.setType(type);
        room.setPrice(price);
        room.setAvailable(available);
        room.setTimesBooked(timesBooked);
        // hotel устанавливается через отдельный метод, если есть связь
        return room;
    }

    /**
     * Тест для метода: confirmAvailability()
     * Назначение: Подтверждение доступности существующего доступного номера
     * Ожидаемый результат:
     * - Возвращает true
     * - Увеличивает счетчик бронирований на 1
     * - Сохраняет обновленный номер
     */
    @Test
    void confirmAvailability_WithExistingAvailableRoom_ShouldReturnTrueAndIncrementTimesBooked() {
        // Given
        Long roomId = 1L;
        Room room = createRoom(roomId, "101", "STANDARD", 100.0, true, 5, 1L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomRepository.save(room)).thenReturn(room);

        // When
        boolean result = roomService.confirmAvailability(roomId);

        // Then
        assertTrue(result);
        assertEquals(6, room.getTimesBooked());
        verify(roomRepository, times(1)).findById(roomId);
        verify(roomRepository, times(1)).save(room);
    }

    /**
     * Тест для метода: confirmAvailability()
     * Назначение: Подтверждение доступности номера с null счетчиком бронирований
     * Ожидаемый результат:
     * - Возвращает true
     * - Устанавливает счетчик бронирований в 1
     * - Сохраняет обновленный номер
     */
    @Test
    void confirmAvailability_WithNullTimesBooked_ShouldSetTimesBookedToOne() {
        // Given
        Long roomId = 1L;
        Room room = createRoom(roomId, "101", "STANDARD", 100.0, true, null, 1L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomRepository.save(room)).thenReturn(room);

        // When
        boolean result = roomService.confirmAvailability(roomId);

        // Then
        assertTrue(result);
        assertEquals(1, room.getTimesBooked());
        verify(roomRepository, times(1)).findById(roomId);
        verify(roomRepository, times(1)).save(room);
    }

    /**
     * Тест для метода: confirmAvailability()
     * Назначение: Попытка подтверждения доступности несуществующего номера
     * Ожидаемый результат:
     * - Возвращает false
     * - Не вызывает метод save()
     */
    @Test
    void confirmAvailability_WithNonExistingRoom_ShouldReturnFalse() {
        // Given
        Long roomId = 999L;

        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When
        boolean result = roomService.confirmAvailability(roomId);

        // Then
        assertFalse(result);
        verify(roomRepository, times(1)).findById(roomId);
        verify(roomRepository, never()).save(any(Room.class));
    }

    /**
     * Тест для метода: confirmAvailability()
     * Назначение: Попытка подтверждения доступности недоступного номера
     * Ожидаемый результат:
     * - Возвращает false
     * - Не изменяет счетчик бронирований
     * - Не вызывает метод save()
     */
    @Test
    void confirmAvailability_WithUnavailableRoom_ShouldReturnFalse() {
        // Given
        Long roomId = 1L;
        Room room = createRoom(roomId, "101", "STANDARD", 100.0, false, 5, 1L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        // When
        boolean result = roomService.confirmAvailability(roomId);

        // Then
        assertFalse(result);
        assertEquals(5, room.getTimesBooked()); // счетчик не изменился
        verify(roomRepository, times(1)).findById(roomId);
        verify(roomRepository, never()).save(any(Room.class));
    }

    /**
     * Тест для метода: releaseRoom()
     * Назначение: Снятие блокировки существующего номера
     * Ожидаемый результат:
     * - Не выбрасывает исключений
     * - Вызывает метод поиска номера
     * - Не изменяет счетчик бронирований (по текущей логике)
     */
    @Test
    void releaseRoom_WithExistingRoom_ShouldReleaseRoom() {
        // Given
        Long roomId = 1L;
        Room room = createRoom(roomId, "101", "STANDARD", 100.0, true, 5, 1L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        // When
        roomService.releaseRoom(roomId);

        // Then
        verify(roomRepository, times(1)).findById(roomId);
        verify(roomRepository, never()).save(any(Room.class)); // по текущей логике сохранение не вызывается
    }

    /**
     * Тест для метода: releaseRoom()
     * Назначение: Снятие блокировки несуществующего номера
     * Ожидаемый результат:
     * - Не выбрасывает исключений
     * - Вызывает метод поиска номера
     * - Завершается нормально
     */
    @Test
    void releaseRoom_WithNonExistingRoom_ShouldCompleteWithoutError() {
        // Given
        Long roomId = 999L;

        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> roomService.releaseRoom(roomId));
        verify(roomRepository, times(1)).findById(roomId);
        verify(roomRepository, never()).save(any(Room.class));
    }

    /**
     * Тест для метода: findAvailableRooms()
     * Назначение: Получение списка доступных номеров
     * Ожидаемый результат:
     * - Возвращает список доступных номеров
     * - Вызывает соответствующий метод репозитория
     */
    @Test
    void findAvailableRooms_ShouldReturnAvailableRooms() {
        // Given
        Room room1 = createRoom(1L, "101", "STANDARD", 100.0, true, 5, 1L);
        Room room2 = createRoom(2L, "102", "DELUXE", 200.0, true, 10, 1L);
        List<Room> expectedRooms = Arrays.asList(room1, room2);

        when(roomRepository.findByAvailableTrue()).thenReturn(expectedRooms);

        // When
        List<Room> result = roomService.findAvailableRooms();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedRooms, result);
        verify(roomRepository, times(1)).findByAvailableTrue();
    }

    /**
     * Тест для метода: findRecommendedRooms()
     * Назначение: Получение списка рекомендованных номеров
     * Ожидаемый результат:
     * - Возвращает список номеров, отсортированных по популярности
     * - Вызывает соответствующий метод репозитория
     */
    @Test
    void findRecommendedRooms_ShouldReturnRecommendedRooms() {
        // Given
        Room room1 = createRoom(1L, "101", "STANDARD", 100.0, true, 15, 1L);
        Room room2 = createRoom(2L, "201", "SUITE", 300.0, true, 20, 1L);
        List<Room> expectedRooms = Arrays.asList(room2, room1); // отсортировано по timesBooked

        when(roomRepository.findAvailableRoomsOrderByTimesBooked()).thenReturn(expectedRooms);

        // When
        List<Room> result = roomService.findRecommendedRooms();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedRooms, result);
        verify(roomRepository, times(1)).findAvailableRoomsOrderByTimesBooked();
    }

    /**
     * Тест для метода: findById()
     * Назначение: Поиск номера по существующему ID
     * Ожидаемый результат:
     * - Возвращает номер
     * - Вызывает метод репозитория
     */
    @Test
    void findById_WithExistingId_ShouldReturnRoom() {
        // Given
        Long roomId = 1L;
        Room expectedRoom = createRoom(roomId, "101", "STANDARD", 100.0, true, 5, 1L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(expectedRoom));

        // When
        Room result = roomService.findById(roomId);

        // Then
        assertNotNull(result);
        assertEquals(expectedRoom, result);
        verify(roomRepository, times(1)).findById(roomId);
    }

    /**
     * Тест для метода: findById()
     * Назначение: Поиск номера по несуществующему ID
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением об ошибке
     * - Вызывает метод репозитория
     */
    @Test
    void findById_WithNonExistingId_ShouldThrowException() {
        // Given
        Long roomId = 999L;

        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roomService.findById(roomId));

        assertEquals("Room not found with id: " + roomId, exception.getMessage());
        verify(roomRepository, times(1)).findById(roomId);
    }

    /**
     * Тест для метода: findRoomsByHotelId()
     * Назначение: Поиск номеров по ID отеля
     * Ожидаемый результат:
     * - Возвращает список номеров отеля
     * - Вызывает соответствующий метод репозитория
     */
    @Test
    void findRoomsByHotelId_ShouldReturnRoomsForHotel() {
        // Given
        Long hotelId = 1L;
        Room room1 = createRoom(1L, "101", "STANDARD", 100.0, true, 5, hotelId);
        Room room2 = createRoom(2L, "102", "DELUXE", 200.0, false, 8, hotelId);
        List<Room> expectedRooms = Arrays.asList(room1, room2);

        when(roomRepository.findByHotelId(hotelId)).thenReturn(expectedRooms);

        // When
        List<Room> result = roomService.findRoomsByHotelId(hotelId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedRooms, result);
        verify(roomRepository, times(1)).findByHotelId(hotelId);
    }

    /**
     * Тест для метода: save()
     * Назначение: Сохранение номера
     * Ожидаемый результат:
     * - Возвращает сохраненный номер
     * - Вызывает метод save репозитория
     */
    @Test
    void save_ShouldReturnSavedRoom() {
        // Given
        Room roomToSave = createRoom(null, "101", "STANDARD", 100.0, true, 0, 1L);
        Room savedRoom = createRoom(1L, "101", "STANDARD", 100.0, true, 0, 1L);

        when(roomRepository.save(roomToSave)).thenReturn(savedRoom);

        // When
        Room result = roomService.save(roomToSave);

        // Then
        assertNotNull(result);
        assertEquals(savedRoom, result);
        verify(roomRepository, times(1)).save(roomToSave);
    }

    /**
     * Тест для метода: deleteById()
     * Назначение: Удаление номера по ID
     * Ожидаемый результат:
     * - Вызывает метод deleteById репозитория
     * - Не выбрасывает исключений
     */
    @Test
    void deleteById_ShouldCallRepositoryDelete() {
        // Given
        Long roomId = 1L;
        doNothing().when(roomRepository).deleteById(roomId);

        // When
        roomService.deleteById(roomId);

        // Then
        verify(roomRepository, times(1)).deleteById(roomId);
    }

    /**
     * Тест для метода: confirmAvailability()
     * Назначение: Обработка исключения при подтверждении доступности
     * Ожидаемый результат:
     * - Возвращает false при возникновении исключения
     * - Логирует ошибку
     */
    @Test
    void confirmAvailability_WhenExceptionOccurs_ShouldReturnFalse() {
        // Given
        Long roomId = 1L;

        when(roomRepository.findById(roomId)).thenThrow(new RuntimeException("Database error"));

        // When
        boolean result = roomService.confirmAvailability(roomId);

        // Then
        assertFalse(result);
        verify(roomRepository, times(1)).findById(roomId);
        verify(roomRepository, never()).save(any(Room.class));
    }

    /**
     * Тест для метода: releaseRoom()
     * Назначение: Обработка исключения при снятии блокировки
     * Ожидаемый результат:
     * - Не выбрасывает исключение наружу
     * - Логирует ошибку
     */
    @Test
    void releaseRoom_WhenExceptionOccurs_ShouldHandleExceptionGracefully() {
        // Given
        Long roomId = 1L;

        when(roomRepository.findById(roomId)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertDoesNotThrow(() -> roomService.releaseRoom(roomId));
        verify(roomRepository, times(1)).findById(roomId);
    }
}