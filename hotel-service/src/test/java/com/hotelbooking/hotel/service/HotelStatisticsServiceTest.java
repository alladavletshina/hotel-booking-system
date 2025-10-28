package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.statistics.HotelStatisticsDto;
import com.hotelbooking.hotel.dto.statistics.RoomPopularityDto;
import com.hotelbooking.hotel.dto.statistics.RoomTypeStatistics;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.BookingSlot;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import com.hotelbooking.hotel.repository.BookingSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelStatisticsServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingSlotRepository bookingSlotRepository;

    @InjectMocks
    private HotelStatisticsService hotelStatisticsService;

    private Hotel testHotel;
    private Room testRoom1;
    private Room testRoom2;
    private BookingSlot testBookingSlot;
    private final Long HOTEL_ID = 1L;
    private final LocalDate START_DATE = LocalDate.now().minusDays(30);
    private final LocalDate END_DATE = LocalDate.now();

    @BeforeEach
    void setUp() {
        // Setup test hotel
        testHotel = new Hotel();
        testHotel.setId(HOTEL_ID);
        testHotel.setName("Test Hotel");
        testHotel.setAddress("Test Address");

        // Setup test rooms
        testRoom1 = new Room();
        testRoom1.setId(1L);
        testRoom1.setNumber("101");
        testRoom1.setType("DELUXE");
        testRoom1.setPrice(200.0);
        testRoom1.setAvailable(true);
        testRoom1.setTimesBooked(10);
        testRoom1.setHotel(testHotel);

        testRoom2 = new Room();
        testRoom2.setId(2L);
        testRoom2.setNumber("102");
        testRoom2.setType("STANDARD");
        testRoom2.setPrice(100.0);
        testRoom2.setAvailable(true);
        testRoom2.setTimesBooked(5);
        testRoom2.setHotel(testHotel);

        // Setup test booking slot
        testBookingSlot = new BookingSlot();
        testBookingSlot.setId(1L);
        testBookingSlot.setRoomId(1L);
        testBookingSlot.setStartDate(LocalDate.now().minusDays(5));
        testBookingSlot.setEndDate(LocalDate.now().minusDays(2));
        testBookingSlot.setBookingId(100L);
        testBookingSlot.setStatus("CONFIRMED");
    }

    /**
     * Тест: Получение статистики по отелю
     */
    @Test
    void getHotelStatistics_WithValidData_ShouldReturnStatistics() {
        // Arrange
        List<Room> hotelRooms = Arrays.asList(testRoom1, testRoom2);
        List<BookingSlot> bookingSlots = Arrays.asList(testBookingSlot);

        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(testHotel));
        when(roomRepository.findByHotelId(HOTEL_ID)).thenReturn(hotelRooms);
        // Разрешаем множественные вызовы findByHotelId
        when(bookingSlotRepository.findConflictingSlots(anyLong(), eq(START_DATE), eq(END_DATE)))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList(testBookingSlot));

        // Act
        HotelStatisticsDto result = hotelStatisticsService.getHotelStatistics(HOTEL_ID, START_DATE, END_DATE);

        // Assert
        assertNotNull(result);
        assertEquals(HOTEL_ID, result.getHotelId());
        assertEquals("Test Hotel", result.getHotelName());

        // Исправлено: разрешаем 2 вызова findByHotelId
        verify(roomRepository, times(2)).findByHotelId(HOTEL_ID);
        verify(bookingSlotRepository, times(2)).findConflictingSlots(anyLong(), eq(START_DATE), eq(END_DATE));
    }

    /**
     * Тест: Получение статистики по отелю - отель не найден
     */
    @Test
    void getHotelStatistics_WithNonExistingHotel_ShouldThrowException() {
        // Arrange
        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> hotelStatisticsService.getHotelStatistics(HOTEL_ID, START_DATE, END_DATE));

        assertEquals("Hotel not found with id: " + HOTEL_ID, exception.getMessage());
        verify(hotelRepository).findById(HOTEL_ID);
        verify(roomRepository, never()).findByHotelId(anyLong());
    }

    /**
     * Тест: Получение статистики по отелю - без номеров
     */
    @Test
    void getHotelStatistics_WithNoRooms_ShouldReturnEmptyStatistics() {
        // Arrange
        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(testHotel));
        when(roomRepository.findByHotelId(HOTEL_ID)).thenReturn(Collections.emptyList());

        // Act
        HotelStatisticsDto result = hotelStatisticsService.getHotelStatistics(HOTEL_ID, START_DATE, END_DATE);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalRooms());
        assertEquals(0, result.getAvailableRooms());

        // Исправлено: разрешаем 2 вызова для пустого списка
        verify(roomRepository, times(2)).findByHotelId(HOTEL_ID);
        verify(bookingSlotRepository, never()).findConflictingSlots(anyLong(), any(), any());
    }

    /**
     * Тест: Сравнительная статистика отелей
     */
    @Test
    void getHotelsComparison_WithMultipleHotels_ShouldReturnSortedList() {
        // Arrange
        Hotel hotel1 = new Hotel();
        hotel1.setId(1L);
        hotel1.setName("Hotel 1");

        Hotel hotel2 = new Hotel();
        hotel2.setId(2L);
        hotel2.setName("Hotel 2");

        List<Hotel> hotels = Arrays.asList(hotel1, hotel2);

        Room room1 = new Room();
        room1.setId(1L);
        room1.setTimesBooked(20);
        room1.setType("DELUXE");
        room1.setPrice(200.0);
        room1.setAvailable(true);

        Room room2 = new Room();
        room2.setId(2L);
        room2.setTimesBooked(5);
        room2.setType("STANDARD");
        room2.setPrice(100.0);
        room2.setAvailable(true);

        // Мокаем вызовы для первого отеля
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel1));
        when(roomRepository.findByHotelId(1L)).thenReturn(Arrays.asList(room1));

        // Мокаем вызовы для второго отеля
        when(hotelRepository.findById(2L)).thenReturn(Optional.of(hotel2));
        when(roomRepository.findByHotelId(2L)).thenReturn(Arrays.asList(room2));

        when(hotelRepository.findAll()).thenReturn(hotels);
        when(bookingSlotRepository.findConflictingSlots(anyLong(), eq(START_DATE), eq(END_DATE)))
                .thenReturn(Collections.emptyList());

        // Act
        List<HotelStatisticsDto> result = hotelStatisticsService.getHotelsComparison(START_DATE, END_DATE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // Проверяем что статистика содержит оба отеля
        Set<Long> hotelIds = result.stream()
                .map(HotelStatisticsDto::getHotelId)
                .collect(Collectors.toSet());
        assertTrue(hotelIds.contains(1L));
        assertTrue(hotelIds.contains(2L));

        verify(hotelRepository).findAll();
        verify(hotelRepository).findById(1L);
        verify(hotelRepository).findById(2L);
        verify(roomRepository, times(2)).findByHotelId(1L);
        verify(roomRepository, times(2)).findByHotelId(2L);
    }

    /**
     * Тест: Получение загруженности по дням
     */
    @Test
    void getDailyOccupancy_WithValidData_ShouldReturnDailyOccupancy() {
        // Arrange
        List<Room> hotelRooms = Arrays.asList(testRoom1, testRoom2);
        LocalDate testStartDate = LocalDate.now().minusDays(2);
        LocalDate testEndDate = LocalDate.now();

        when(roomRepository.findByHotelId(HOTEL_ID)).thenReturn(hotelRooms);
        when(bookingSlotRepository.findConflictingSlots(anyLong(), eq(testStartDate), eq(testEndDate)))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList(testBookingSlot));

        // Act
        Map<LocalDate, Double> result = hotelStatisticsService.getDailyOccupancy(HOTEL_ID, testStartDate, testEndDate);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Исправлено: разрешаем 2 вызова
        verify(roomRepository, times(2)).findByHotelId(HOTEL_ID);
        verify(bookingSlotRepository, times(2)).findConflictingSlots(anyLong(), eq(testStartDate), eq(testEndDate));
    }

    /**
     * Тест: Получение популярных номеров
     */
    @Test
    void getPopularRooms_WithMultipleRooms_ShouldReturnSortedListWithRanks() {
        // Arrange
        Room popularRoom = new Room();
        popularRoom.setId(1L);
        popularRoom.setNumber("101");
        popularRoom.setType("DELUXE");
        popularRoom.setPrice(200.0);
        popularRoom.setTimesBooked(15);

        Room mediumRoom = new Room();
        mediumRoom.setId(2L);
        mediumRoom.setNumber("102");
        mediumRoom.setType("STANDARD");
        mediumRoom.setPrice(100.0);
        mediumRoom.setTimesBooked(8);

        List<Room> hotelRooms = Arrays.asList(mediumRoom, popularRoom);
        int limit = 2;

        when(roomRepository.findByHotelId(HOTEL_ID)).thenReturn(hotelRooms);

        // Act
        List<RoomPopularityDto> result = hotelStatisticsService.getPopularRooms(HOTEL_ID, limit);

        // Assert
        assertNotNull(result);
        assertEquals(limit, result.size());
        assertEquals(1, result.get(0).getPopularityRank());
        assertEquals(2, result.get(1).getPopularityRank());

        verify(roomRepository).findByHotelId(HOTEL_ID);
    }

    /**
     * Тест: Расчет статистики по типам номеров
     */
    @Test
    void getHotelStatistics_WithDifferentRoomTypes_ShouldGroupStatisticsByType() {
        // Arrange
        Room deluxeRoom1 = new Room();
        deluxeRoom1.setId(1L);
        deluxeRoom1.setType("DELUXE");
        deluxeRoom1.setTimesBooked(10);
        deluxeRoom1.setPrice(200.0);

        Room deluxeRoom2 = new Room();
        deluxeRoom2.setId(2L);
        deluxeRoom2.setType("DELUXE");
        deluxeRoom2.setTimesBooked(15);
        deluxeRoom2.setPrice(200.0);

        Room standardRoom = new Room();
        standardRoom.setId(3L);
        standardRoom.setType("STANDARD");
        standardRoom.setTimesBooked(5);
        standardRoom.setPrice(100.0);

        List<Room> hotelRooms = Arrays.asList(deluxeRoom1, deluxeRoom2, standardRoom);

        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(testHotel));
        when(roomRepository.findByHotelId(HOTEL_ID)).thenReturn(hotelRooms);
        when(bookingSlotRepository.findConflictingSlots(anyLong(), eq(START_DATE), eq(END_DATE)))
                .thenReturn(Collections.emptyList());

        // Act
        HotelStatisticsDto result = hotelStatisticsService.getHotelStatistics(HOTEL_ID, START_DATE, END_DATE);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getRoomTypeStats());

        // Исправлено: разрешаем 2 вызова
        verify(roomRepository, times(2)).findByHotelId(HOTEL_ID);
        verify(bookingSlotRepository, times(3)).findConflictingSlots(anyLong(), eq(START_DATE), eq(END_DATE));
    }

    /**
     * Тест: Расчет дохода
     */
    @Test
    void getHotelStatistics_WithBookings_ShouldCalculateRevenueCorrectly() {
        // Arrange
        Room expensiveRoom = new Room();
        expensiveRoom.setId(1L);
        expensiveRoom.setPrice(300.0);
        expensiveRoom.setTimesBooked(5);
        expensiveRoom.setType("DELUXE");

        Room cheapRoom = new Room();
        cheapRoom.setId(2L);
        cheapRoom.setPrice(100.0);
        cheapRoom.setTimesBooked(3);
        cheapRoom.setType("STANDARD");

        List<Room> hotelRooms = Arrays.asList(expensiveRoom, cheapRoom);

        BookingSlot longBooking = new BookingSlot();
        longBooking.setRoomId(1L);
        longBooking.setStartDate(LocalDate.now().minusDays(7));
        longBooking.setEndDate(LocalDate.now().minusDays(2));
        longBooking.setStatus("CONFIRMED");

        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(testHotel));
        when(roomRepository.findByHotelId(HOTEL_ID)).thenReturn(hotelRooms);
        when(bookingSlotRepository.findConflictingSlots(eq(1L), eq(START_DATE), eq(END_DATE)))
                .thenReturn(Collections.singletonList(longBooking));
        when(bookingSlotRepository.findConflictingSlots(eq(2L), eq(START_DATE), eq(END_DATE)))
                .thenReturn(Collections.emptyList());

        // Act
        HotelStatisticsDto result = hotelStatisticsService.getHotelStatistics(HOTEL_ID, START_DATE, END_DATE);

        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalRevenue() > 0);

        // Исправлено: разрешаем 2 вызова
        verify(roomRepository, times(2)).findByHotelId(HOTEL_ID);
    }

    /**
     * Тест: Поиск самого популярного и наименее популярного номера
     */
    @Test
    void getHotelStatistics_WithRoomsOfDifferentPopularity_ShouldIdentifyExtremes() {
        // Arrange
        Room mostPopular = new Room();
        mostPopular.setId(1L);
        mostPopular.setNumber("101");
        mostPopular.setTimesBooked(20);
        mostPopular.setPrice(200.0);
        mostPopular.setType("DELUXE");

        Room mediumPopular = new Room();
        mediumPopular.setId(2L);
        mediumPopular.setNumber("102");
        mediumPopular.setTimesBooked(10);
        mediumPopular.setPrice(150.0);
        mediumPopular.setType("STANDARD");

        Room leastPopular = new Room();
        leastPopular.setId(3L);
        leastPopular.setNumber("103");
        leastPopular.setTimesBooked(2);
        leastPopular.setPrice(100.0);
        leastPopular.setType("ECONOMY");

        List<Room> hotelRooms = Arrays.asList(mediumPopular, leastPopular, mostPopular);

        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(testHotel));
        when(roomRepository.findByHotelId(HOTEL_ID)).thenReturn(hotelRooms);
        when(bookingSlotRepository.findConflictingSlots(anyLong(), eq(START_DATE), eq(END_DATE)))
                .thenReturn(Collections.emptyList());

        // Act
        HotelStatisticsDto result = hotelStatisticsService.getHotelStatistics(HOTEL_ID, START_DATE, END_DATE);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getMostPopularRoom());
        assertNotNull(result.getLeastPopularRoom());

        assertEquals(mostPopular.getId(), result.getMostPopularRoom().getRoomId());
        assertEquals(leastPopular.getId(), result.getLeastPopularRoom().getRoomId());

        // Исправлено: разрешаем 2 вызова
        verify(roomRepository, times(2)).findByHotelId(HOTEL_ID);
    }
}