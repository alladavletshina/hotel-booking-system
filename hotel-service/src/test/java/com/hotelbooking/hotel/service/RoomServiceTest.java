package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.entity.BookingSlot;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.repository.BookingSlotRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingSlotRepository bookingSlotRepository;

    @InjectMocks
    private RoomService roomService;

    private Room testRoom;
    private BookingSlot testBookingSlot;
    private final Long ROOM_ID = 1L;
    private final Long BOOKING_ID = 100L;
    private final LocalDate START_DATE = LocalDate.now().plusDays(1);
    private final LocalDate END_DATE = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        testRoom = new Room();
        testRoom.setId(ROOM_ID);
        testRoom.setType("DELUXE");
        testRoom.setPrice(200.0);
        testRoom.setAvailable(true);
        testRoom.setTimesBooked(5);

        testBookingSlot = new BookingSlot();
        testBookingSlot.setId(1L);
        testBookingSlot.setRoomId(ROOM_ID);
        testBookingSlot.setStartDate(START_DATE);
        testBookingSlot.setEndDate(END_DATE);
        testBookingSlot.setBookingId(BOOKING_ID);
        testBookingSlot.setStatus("RESERVED");
    }

    /**
     * Тест для метода: isRoomAvailable
     * Назначение: Проверка доступности номера на указанные даты
     * Сценарий: Номер доступен и нет конфликтов по датам
     * Ожидаемый результат:
     * - Возвращает true
     * - Номер существует и доступен
     * - Нет конфликтов бронирований
     * Бизнес-логика:
     * 1. Проверяет существование номера
     * 2. Проверяет флаг available номера
     * 3. Проверяет конфликты по датам через репозиторий
     */
    @Test
    void isRoomAvailable_WithAvailableRoomAndNoConflicts_ShouldReturnTrue() {

        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(testRoom));
        when(bookingSlotRepository.hasDateConflict(ROOM_ID, START_DATE, END_DATE)).thenReturn(false);

        boolean result = roomService.isRoomAvailable(ROOM_ID, START_DATE, END_DATE);

        assertTrue(result);
        verify(roomRepository).findById(ROOM_ID);
        verify(bookingSlotRepository).hasDateConflict(ROOM_ID, START_DATE, END_DATE);
    }

    /**
     * Тест для метода: isRoomAvailable
     * Назначение: Проверка доступности номера на указанные даты
     * Сценарий: Номер не существует
     * Ожидаемый результат:
     * - Возвращает false
     * - Номер не найден в репозитории
     * Бизнес-логика:
     * 1. Ищет номер по ID
     * 2. При отсутствии номера возвращает false
     */
    @Test
    void isRoomAvailable_WithNonExistingRoom_ShouldReturnFalse() {

        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        boolean result = roomService.isRoomAvailable(ROOM_ID, START_DATE, END_DATE);

        assertFalse(result);
        verify(roomRepository).findById(ROOM_ID);
        verify(bookingSlotRepository, never()).hasDateConflict(anyLong(), any(), any());
    }

    /**
     * Тест для метода: isRoomAvailable
     * Назначение: Проверка доступности номера на указанные даты
     * Сценарий: Номер существует но недоступен
     * Ожидаемый результат:
     * - Возвращает false
     * - Флаг available номера установлен в false
     * Бизнес-логика:
     * 1. Находит номер по ID
     * 2. Проверяет флаг available
     * 3. При unavailable возвращает false
     */
    @Test
    void isRoomAvailable_WithUnavailableRoom_ShouldReturnFalse() {

        testRoom.setAvailable(false);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(testRoom));

        boolean result = roomService.isRoomAvailable(ROOM_ID, START_DATE, END_DATE);

        assertFalse(result);
        verify(roomRepository).findById(ROOM_ID);
        verify(bookingSlotRepository, never()).hasDateConflict(anyLong(), any(), any());
    }

    /**
     * Тест для метода: isRoomAvailable
     * Назначение: Проверка доступности номера на указанные даты
     * Сценарий: Номер доступен но есть конфликты по датам
     * Ожидаемый результат:
     * - Возвращает false
     * - Обнаружены конфликтующие бронирования
     * Бизнес-логика:
     * 1. Проверяет доступность номера
     * 2. Проверяет конфликты по датам
     * 3. При наличии конфликтов возвращает false
     */
    @Test
    void isRoomAvailable_WithDateConflict_ShouldReturnFalse() {

        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(testRoom));
        when(bookingSlotRepository.hasDateConflict(ROOM_ID, START_DATE, END_DATE)).thenReturn(true);

        boolean result = roomService.isRoomAvailable(ROOM_ID, START_DATE, END_DATE);

        assertFalse(result);
        verify(roomRepository).findById(ROOM_ID);
        verify(bookingSlotRepository).hasDateConflict(ROOM_ID, START_DATE, END_DATE);
    }

    /**
     * Тест для метода: confirmAvailability
     * Назначение: Подтверждение доступности с временной блокировкой
     * Сценарий: Успешное подтверждение доступности
     * Ожидаемый результат:
     * - Возвращает true
     * - Создается временная блокировка
     * - Увеличивается счетчик бронирований
     * Бизнес-логика:
     * 1. Проверяет базовую доступность номера
     * 2. Проверяет конфликты по датам
     * 3. Создает временный слот бронирования
     * 4. Увеличивает счетчик timesBooked
     */
    @Test
    void confirmAvailability_WithAvailableRoom_ShouldReturnTrueAndCreateReservation() {

        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(testRoom));
        when(bookingSlotRepository.hasDateConflict(ROOM_ID, START_DATE, END_DATE)).thenReturn(false);
        when(bookingSlotRepository.save(any(BookingSlot.class))).thenReturn(testBookingSlot);
        when(roomRepository.save(testRoom)).thenReturn(testRoom);

        boolean result = roomService.confirmAvailability(ROOM_ID, START_DATE, END_DATE, BOOKING_ID);

        assertTrue(result);
        verify(roomRepository).findById(ROOM_ID);
        verify(bookingSlotRepository).hasDateConflict(ROOM_ID, START_DATE, END_DATE);
        verify(bookingSlotRepository).save(any(BookingSlot.class));
        verify(roomRepository).save(testRoom);
        assertEquals(6, testRoom.getTimesBooked()); // 5 + 1
    }

    /**
     * Тест для метода: confirmAvailability
     * Назначение: Подтверждение доступности с временной блокировкой
     * Сценарий: Номер недоступен
     * Ожидаемый результат:
     * - Возвращает false
     * - Не создает временную блокировку
     * Бизнес-логика:
     * 1. Проверяет доступность номера
     * 2. При недоступности возвращает false
     * 3. Не выполняет дальнейшие действия
     */
    @Test
    void confirmAvailability_WithUnavailableRoom_ShouldReturnFalse() {

        testRoom.setAvailable(false);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(testRoom));

        boolean result = roomService.confirmAvailability(ROOM_ID, START_DATE, END_DATE, BOOKING_ID);

        assertFalse(result);
        verify(roomRepository).findById(ROOM_ID);
        verify(bookingSlotRepository, never()).hasDateConflict(anyLong(), any(), any());
        verify(bookingSlotRepository, never()).save(any(BookingSlot.class));
    }

    /**
     * Тест для метода: confirmAvailability
     * Назначение: Подтверждение доступности с временной блокировкой
     * Сценарий: Обнаружены конфликты по датам
     * Ожидаемый результат:
     * - Возвращает false
     * - Не создает временную блокировку
     * Бизнес-логика:
     * 1. Проверяет доступность номера
     * 2. Проверяет конфликты по датам
     * 3. При наличии конфликтов возвращает false
     */
    @Test
    void confirmAvailability_WithDateConflict_ShouldReturnFalse() {

        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(testRoom));
        when(bookingSlotRepository.hasDateConflict(ROOM_ID, START_DATE, END_DATE)).thenReturn(true);

        boolean result = roomService.confirmAvailability(ROOM_ID, START_DATE, END_DATE, BOOKING_ID);

        assertFalse(result);
        verify(roomRepository).findById(ROOM_ID);
        verify(bookingSlotRepository).hasDateConflict(ROOM_ID, START_DATE, END_DATE);
        verify(bookingSlotRepository, never()).save(any(BookingSlot.class));
    }

    /**
     * Тест для метода: releaseRoom
     * Назначение: Освобождение номера (компенсирующее действие)
     * Сценарий: Успешное освобождение временных слотов
     * Ожидаемый результат:
     * - Удаляет временные слоты бронирования
     * - Завершается без исключений
     * Бизнес-логика:
     * 1. Находит все слоты по bookingId
     * 2. Удаляет слоты со статусом RESERVED
     * 3. Логирует количество освобожденных слотов
     */
    @Test
    void releaseRoom_WithReservedSlots_ShouldRemoveTemporarySlots() {

        when(bookingSlotRepository.findByBookingId(BOOKING_ID)).thenReturn(Collections.singletonList(testBookingSlot));

        roomService.releaseRoom(ROOM_ID, BOOKING_ID);

        verify(bookingSlotRepository).findByBookingId(BOOKING_ID);
        verify(bookingSlotRepository).delete(testBookingSlot);
    }

    /**
     * Тест для метода: releaseRoom
     * Назначение: Освобождение номера (компенсирующее действие)
     * Сценарий: Нет временных слотов для освобождения
     * Ожидаемый результат:
     * - Завершается без исключений
     * - Не выполняет операций удаления
     * Бизнес-логика:
     * 1. Ищет слоты по bookingId
     * 2. При отсутствии слотов завершает работу
     */
    @Test
    void releaseRoom_WithNoReservedSlots_ShouldCompleteWithoutAction() {

        when(bookingSlotRepository.findByBookingId(BOOKING_ID)).thenReturn(List.of());

        roomService.releaseRoom(ROOM_ID, BOOKING_ID);

        verify(bookingSlotRepository).findByBookingId(BOOKING_ID);
        verify(bookingSlotRepository, never()).delete(any(BookingSlot.class));
    }

    /**
     * Тест для метода: confirmBooking
     * Назначение: Подтверждение бронирования
     * Сценарий: Успешное подтверждение временных слотов
     * Ожидаемый результат:
     * - Изменяет статус слотов с RESERVED на CONFIRMED
     * - Сохраняет обновленные слоты
     * Бизнес-логика:
     * 1. Находит все слоты по bookingId
     * 2. Для слотов со статусом RESERVED меняет статус на CONFIRMED
     * 3. Сохраняет изменения
     */
    @Test
    void confirmBooking_WithReservedSlots_ShouldConfirmSlots() {

        when(bookingSlotRepository.findByBookingId(BOOKING_ID)).thenReturn(Collections.singletonList(testBookingSlot));
        when(bookingSlotRepository.save(testBookingSlot)).thenReturn(testBookingSlot);

        roomService.confirmBooking(ROOM_ID, BOOKING_ID);

        verify(bookingSlotRepository).findByBookingId(BOOKING_ID);
        verify(bookingSlotRepository).save(testBookingSlot);
        assertEquals("CONFIRMED", testBookingSlot.getStatus());
    }

    /**
     * Тест для метода: cancelBooking
     * Назначение: Отмена бронирования
     * Сценарий: Успешная отмена всех слотов бронирования
     * Ожидаемый результат:
     * - Изменяет статус всех слотов на CANCELLED
     * - Сохраняет обновленные слоты
     * Бизнес-логика:
     * 1. Находит все слоты по bookingId
     * 2. Устанавливает статус CANCELLED для всех слотов
     * 3. Сохраняет изменения
     */
    @Test
    void cancelBooking_WithBookingSlots_ShouldCancelAllSlots() {

        BookingSlot slot1 = new BookingSlot();
        slot1.setStatus("RESERVED");
        BookingSlot slot2 = new BookingSlot();
        slot2.setStatus("CONFIRMED");

        when(bookingSlotRepository.findByBookingId(BOOKING_ID)).thenReturn(Arrays.asList(slot1, slot2));
        when(bookingSlotRepository.save(any(BookingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        roomService.cancelBooking(ROOM_ID, BOOKING_ID);

        verify(bookingSlotRepository).findByBookingId(BOOKING_ID);
        verify(bookingSlotRepository, times(2)).save(any(BookingSlot.class));
        assertEquals("CANCELLED", slot1.getStatus());
        assertEquals("CANCELLED", slot2.getStatus());
    }

    /**
     * Тест для метода: findAvailableRooms
     * Назначение: Поиск доступных номеров на указанные даты
     * Сценарий: Найдены доступные номера без конфликтов
     * Ожидаемый результат:
     * - Возвращает список доступных номеров
     * - Фильтрует номера с конфликтами по датам
     * Бизнес-логика:
     * 1. Получает все доступные номера
     * 2. Фильтрует номера без конфликтов по датам
     * 3. Возвращает отфильтрованный список
     */
    @Test
    void findAvailableRooms_WithAvailableRooms_ShouldReturnFilteredList() {

        Room room1 = new Room();
        room1.setId(1L);
        room1.setAvailable(true);

        Room room2 = new Room();
        room2.setId(2L);
        room2.setAvailable(true);

        List<Room> allRooms = Arrays.asList(room1, room2);

        when(roomRepository.findByAvailableTrue()).thenReturn(allRooms);
        when(bookingSlotRepository.hasDateConflict(1L, START_DATE, END_DATE)).thenReturn(false);
        when(bookingSlotRepository.hasDateConflict(2L, START_DATE, END_DATE)).thenReturn(true);

        List<Room> result = roomService.findAvailableRooms(START_DATE, END_DATE);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(roomRepository).findByAvailableTrue();
        verify(bookingSlotRepository, times(2)).hasDateConflict(anyLong(), eq(START_DATE), eq(END_DATE));
    }

    /**
     * Тест для метода: findRecommendedRooms
     * Назначение: Получение рекомендованных номеров на указанные даты
     * Сценарий: Найдены доступные номера для рекомендаций
     * Ожидаемый результат:
     * - Возвращает отсортированный список номеров
     * - Сортирует по количеству бронирований (наименее популярные сначала)
     * Бизнес-логика:
     * 1. Получает доступные номера на даты
     * 2. Сортирует по timesBooked (возрастание)
     * 3. Возвращает отсортированный список
     */
    @Test
    void findRecommendedRooms_WithAvailableRooms_ShouldReturnSortedList() {

        Room room1 = new Room();
        room1.setId(1L);
        room1.setTimesBooked(10);

        Room room2 = new Room();
        room2.setId(2L);
        room2.setTimesBooked(5);

        Room room3 = new Room();
        room3.setId(3L);
        room3.setTimesBooked(15);

        List<Room> availableRooms = Arrays.asList(room1, room2, room3);

        when(roomRepository.findByAvailableTrue()).thenReturn(availableRooms);
        when(bookingSlotRepository.hasDateConflict(anyLong(), any(), any())).thenReturn(false);

        List<Room> result = roomService.findRecommendedRooms(START_DATE, END_DATE);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(2L, result.get(0).getId()); // Least booked (5)
        assertEquals(1L, result.get(1).getId()); // Middle (10)
        assertEquals(3L, result.get(2).getId()); // Most booked (15)
    }

    /**
     * Тест для метода: findBestAvailableRoom
     * Назначение: Автоматический подбор лучшей комнаты
     * Сценарий: Успешный подбор наименее популярной комнаты
     * Ожидаемый результат:
     * - Возвращает первую комнату из рекомендованных
     * - Комната является наименее популярной
     * Бизнес-логика:
     * 1. Получает рекомендованные номера (отсортированные)
     * 2. Выбирает первую комнату из списка
     * 3. Возвращает выбранную комнату
     */
    @Test
    void findBestAvailableRoom_WithAvailableRooms_ShouldReturnLeastPopularRoom() {

        Room leastPopular = new Room();
        leastPopular.setId(1L);
        leastPopular.setTimesBooked(2);

        Room popular = new Room();
        popular.setId(2L);
        popular.setTimesBooked(10);

        List<Room> recommendedRooms = Arrays.asList(leastPopular, popular);

        when(roomRepository.findByAvailableTrue()).thenReturn(recommendedRooms);
        when(bookingSlotRepository.hasDateConflict(anyLong(), any(), any())).thenReturn(false);

        Room result = roomService.findBestAvailableRoom(START_DATE, END_DATE);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(2, result.getTimesBooked());
    }

    /**
     * Тест для метода: findBestAvailableRoom
     * Назначение: Автоматический подбор лучшей комнаты
     * Сценарий: Нет доступных комнат
     * Ожидаемый результат:
     * - Выбрасывает исключение RuntimeException
     * - Сообщение указывает на отсутствие доступных комнат
     * Бизнес-логика:
     * 1. Получает рекомендованные номера
     * 2. При пустом списке выбрасывает исключение
     */
    @Test
    void findBestAvailableRoom_WithNoAvailableRooms_ShouldThrowException() {

        when(roomRepository.findByAvailableTrue()).thenReturn(List.of());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roomService.findBestAvailableRoom(START_DATE, END_DATE));

        assertTrue(exception.getMessage().contains("No available rooms found"));
    }

    /**
     * Тест для метода: findTopAvailableRooms
     * Назначение: Найти несколько лучших вариантов для выбора
     * Сценарий: Запрос ограниченного количества комнат
     * Ожидаемый результат:
     * - Возвращает ограниченное количество комнат
     * - Сохраняет порядок рекомендаций
     * Бизнес-логика:
     * 1. Получает рекомендованные номера
     * 2. Ограничивает результат указанным лимитом
     * 3. Возвращает подсписок
     */
    @Test
    void findTopAvailableRooms_WithLimit_ShouldReturnLimitedList() {

        Room room1 = new Room(); room1.setId(1L); room1.setTimesBooked(1);
        Room room2 = new Room(); room2.setId(2L); room2.setTimesBooked(2);
        Room room3 = new Room(); room3.setId(3L); room3.setTimesBooked(3);
        Room room4 = new Room(); room4.setId(4L); room4.setTimesBooked(4);

        List<Room> allRooms = Arrays.asList(room1, room2, room3, room4);
        int limit = 2;

        when(roomRepository.findByAvailableTrue()).thenReturn(allRooms);
        when(bookingSlotRepository.hasDateConflict(anyLong(), any(), any())).thenReturn(false);

        List<Room> result = roomService.findTopAvailableRooms(START_DATE, END_DATE, limit);

        assertNotNull(result);
        assertEquals(limit, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    /**
     * Тест для метода: validateDates
     * Назначение: Валидация дат бронирования
     * Сценарий: Некорректные даты (начальная дата в прошлом)
     * Ожидаемый результат:
     * - Выбрасывает IllegalArgumentException
     * - Сообщение указывает на ошибку даты
     * Бизнес-логика:
     * 1. Проверяет что начальная дата не в прошлом
     * 2. При нарушении выбрасывает исключение
     */
    @Test
    void validateDates_WithStartDateInPast_ShouldThrowException() {

        LocalDate pastDate = LocalDate.now().minusDays(1);
        LocalDate futureDate = LocalDate.now().plusDays(2);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roomService.findAvailableRooms(pastDate, futureDate));

        assertTrue(exception.getMessage().contains("Start date cannot be in the past"));
    }

    /**
     * Тест для метода: findById
     * Назначение: Получение номера по ID
     * Сценарий: Успешное получение существующего номера
     * Ожидаемый результат:
     * - Возвращает номер с указанным ID
     * - Номер содержит ожидаемые данные
     * Бизнес-логика:
     * 1. Ищет номер в репозитории по ID
     * 2. Возвращает найденный номер
     */
    @Test
    void findById_WithExistingId_ShouldReturnRoom() {

        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(testRoom));

        Room result = roomService.findById(ROOM_ID);

        assertNotNull(result);
        assertEquals(ROOM_ID, result.getId());
        assertEquals("DELUXE", result.getType());
        verify(roomRepository).findById(ROOM_ID);
    }

    /**
     * Тест для метода: findById
     * Назначение: Получение номера по ID
     * Сценарий: Номер с указанным ID не найден
     * Ожидаемый результат:
     * - Выбрасывает исключение RuntimeException
     * - Сообщение содержит ID номера
     * Бизнес-логика:
     * 1. Ищет номер в репозитории по ID
     * 2. При отсутствии номера выбрасывает исключение
     */
    @Test
    void findById_WithNonExistingId_ShouldThrowException() {

        Long nonExistingId = 999L;
        when(roomRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roomService.findById(nonExistingId));

        assertEquals("Room not found with id: " + nonExistingId, exception.getMessage());
        verify(roomRepository).findById(nonExistingId);
    }
}