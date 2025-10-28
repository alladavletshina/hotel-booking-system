package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.statistics.HotelStatisticsDto;
import com.hotelbooking.hotel.dto.statistics.RoomPopularityDto;
import com.hotelbooking.hotel.service.HotelStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelStatisticsControllerTest {

    @Mock
    private HotelStatisticsService hotelStatisticsService;

    @InjectMocks
    private HotelStatisticsController hotelStatisticsController;

    private LocalDate startDate;
    private LocalDate endDate;
    private Long hotelId;
    private HotelStatisticsDto hotelStatisticsDto;
    private RoomPopularityDto roomPopularityDto;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2024, 1, 1);
        endDate = LocalDate.of(2024, 1, 31);
        hotelId = 1L;

        hotelStatisticsDto = new HotelStatisticsDto();
        hotelStatisticsDto.setHotelId(hotelId);
        hotelStatisticsDto.setHotelName("Test Hotel");
        hotelStatisticsDto.setOccupancyRate(75.5);

        roomPopularityDto = new RoomPopularityDto();
        roomPopularityDto.setRoomId(101L);
        roomPopularityDto.setRoomNumber("101");
        roomPopularityDto.setTimesBooked(10);
        roomPopularityDto.setPopularityRank(1);
    }

    /**
     * Тест для endpoint: GET /hotel/{hotelId}/statistics
     * Назначение: Получение расширенной статистики по отелю за указанный период
     * Сценарий: Успешное получение статистики при валидных параметрах
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getHotelStatistics_ShouldReturnStatistics_WhenValidRequest() {
        // Arrange
        when(hotelStatisticsService.getHotelStatistics(eq(hotelId), eq(startDate), eq(endDate)))
                .thenReturn(hotelStatisticsDto);

        // Act
        ResponseEntity<HotelStatisticsDto> response = hotelStatisticsController
                .getHotelStatistics(hotelId, startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(hotelId, response.getBody().getHotelId());

        verify(hotelStatisticsService, times(1))
                .getHotelStatistics(hotelId, startDate, endDate);
    }

    /**
     * Тест для endpoint: GET /hotel/statistics/comparison
     * Назначение: Получение сравнительной статистики по всем отелям
     * Сценарий: Успешное получение списка сравнения отелей
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getHotelsComparison_ShouldReturnComparisonList_WhenValidRequest() {
        // Arrange
        List<HotelStatisticsDto> comparisonList = Arrays.asList(hotelStatisticsDto);

        when(hotelStatisticsService.getHotelsComparison(eq(startDate), eq(endDate)))
                .thenReturn(comparisonList);

        // Act
        ResponseEntity<List<HotelStatisticsDto>> response = hotelStatisticsController
                .getHotelsComparison(startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(hotelStatisticsService, times(1))
                .getHotelsComparison(startDate, endDate);
    }

    /**
     * Тест для endpoint: GET /hotel/{hotelId}/occupancy-daily
     * Назначение: Получение детальной статистики загруженности по дням для отеля
     * Сценарий: Успешное получение ежедневной загруженности
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getDailyOccupancy_ShouldReturnDailyOccupancyMap_WhenValidRequest() {
        // Arrange
        Map<LocalDate, Double> dailyOccupancy = new HashMap<>();
        dailyOccupancy.put(LocalDate.of(2024, 1, 1), 80.0);

        when(hotelStatisticsService.getDailyOccupancy(eq(hotelId), eq(startDate), eq(endDate)))
                .thenReturn(dailyOccupancy);

        // Act
        ResponseEntity<Map<LocalDate, Double>> response = hotelStatisticsController
                .getDailyOccupancy(hotelId, startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(hotelStatisticsService, times(1))
                .getDailyOccupancy(hotelId, startDate, endDate);
    }

    /**
     * Тест для endpoint: GET /hotel/{hotelId}/popular-rooms
     * Назначение: Получение топа самых популярных номеров отеля
     * Сценарий: Успешное получение списка популярных номеров с указанным лимитом
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getPopularRooms_ShouldReturnPopularRoomsList_WhenValidRequest() {
        // Arrange
        List<RoomPopularityDto> popularRooms = Arrays.asList(roomPopularityDto);
        Integer limit = 5;

        when(hotelStatisticsService.getPopularRooms(eq(hotelId), eq(limit)))
                .thenReturn(popularRooms);

        // Act
        ResponseEntity<List<RoomPopularityDto>> response = hotelStatisticsController
                .getPopularRooms(hotelId, limit);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(hotelStatisticsService, times(1))
                .getPopularRooms(hotelId, limit);
    }

    /**
     * Тест для endpoint: GET /hotel/{hotelId}/popular-rooms
     * Назначение: Получение топа самых популярных номеров отеля
     * Сценарий: Обработка null значения лимита
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getPopularRooms_ShouldHandleNullLimit_WhenLimitNotProvided() {
        // Arrange
        List<RoomPopularityDto> popularRooms = Arrays.asList(roomPopularityDto);

        // Когда limit = null, сервис должен получить null
        when(hotelStatisticsService.getPopularRooms(eq(hotelId), isNull()))
                .thenReturn(popularRooms);

        // Act - передаем null
        ResponseEntity<List<RoomPopularityDto>> response = hotelStatisticsController
                .getPopularRooms(hotelId, null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Проверяем, что сервис был вызван с null
        verify(hotelStatisticsService, times(1))
                .getPopularRooms(hotelId, null);
    }

    /**
     * Тест для endpoint: GET /hotel/{hotelId}/popular-rooms
     * Назначение: Получение топа самых популярных номеров отеля
     * Сценарий: Использование значения лимита по умолчанию (10)
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getPopularRooms_ShouldUseDefaultLimit_WhenLimitNotProvided() {
        // Arrange
        List<RoomPopularityDto> popularRooms = Arrays.asList(roomPopularityDto);

        // Проверяем, что при вызове без параметра используется defaultValue = 10
        when(hotelStatisticsService.getPopularRooms(eq(hotelId), eq(10)))
                .thenReturn(popularRooms);

        // Act - вызываем без явного указания limit (будет использовано defaultValue)
        ResponseEntity<List<RoomPopularityDto>> response = hotelStatisticsController
                .getPopularRooms(hotelId, 10);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(hotelStatisticsService, times(1))
                .getPopularRooms(hotelId, 10);
    }

    /**
     * Тест для endpoint: GET /hotel/{hotelId}/popular-rooms
     * Назначение: Получение топа самых популярных номеров отеля
     * Сценарий: Использование пользовательского значения лимита
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getPopularRooms_ShouldUseProvidedLimit_WhenLimitProvided() {
        // Arrange
        List<RoomPopularityDto> popularRooms = Arrays.asList(roomPopularityDto);
        Integer customLimit = 20;

        when(hotelStatisticsService.getPopularRooms(eq(hotelId), eq(customLimit)))
                .thenReturn(popularRooms);

        // Act
        ResponseEntity<List<RoomPopularityDto>> response = hotelStatisticsController
                .getPopularRooms(hotelId, customLimit);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(hotelStatisticsService, times(1))
                .getPopularRooms(hotelId, customLimit);
    }

    /**
     * Тест для endpoint: GET /hotel/{hotelId}/statistics
     * Назначение: Получение расширенной статистики по отелю за указанный период
     * Сценарий: Обработка исключений из сервисного слоя
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getHotelStatistics_ShouldHandleServiceExceptions() {
        // Arrange
        when(hotelStatisticsService.getHotelStatistics(eq(hotelId), eq(startDate), eq(endDate)))
                .thenThrow(new RuntimeException("Hotel not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            hotelStatisticsController.getHotelStatistics(hotelId, startDate, endDate);
        });

        verify(hotelStatisticsService, times(1))
                .getHotelStatistics(hotelId, startDate, endDate);
    }

    /**
     * Тест для endpoint: GET /hotel/{hotelId}/occupancy-daily
     * Назначение: Получение детальной статистики загруженности по дням для отеля
     * Сценарий: Возврат пустой карты при отсутствии данных
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getDailyOccupancy_ShouldReturnEmptyMap_WhenNoData() {
        // Arrange
        Map<LocalDate, Double> emptyOccupancy = Collections.emptyMap();

        when(hotelStatisticsService.getDailyOccupancy(eq(hotelId), eq(startDate), eq(endDate)))
                .thenReturn(emptyOccupancy);

        // Act
        ResponseEntity<Map<LocalDate, Double>> response = hotelStatisticsController
                .getDailyOccupancy(hotelId, startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(hotelStatisticsService, times(1))
                .getDailyOccupancy(hotelId, startDate, endDate);
    }

    /**
     * Тест для endpoint: GET /hotel/{hotelId}/popular-rooms
     * Назначение: Получение топа самых популярных номеров отеля
     * Сценарий: Возврат пустого списка при отсутствии номеров
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getPopularRooms_ShouldReturnEmptyList_WhenNoRooms() {
        // Arrange
        List<RoomPopularityDto> emptyList = Collections.emptyList();

        when(hotelStatisticsService.getPopularRooms(eq(hotelId), eq(10)))
                .thenReturn(emptyList);

        // Act
        ResponseEntity<List<RoomPopularityDto>> response = hotelStatisticsController
                .getPopularRooms(hotelId, 10);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(hotelStatisticsService, times(1))
                .getPopularRooms(hotelId, 10);
    }

    /**
     * Тест для endpoint: GET /hotel/{hotelId}/popular-rooms
     * Назначение: Получение топа самых популярных номеров отеля
     * Сценарий: Обработка нулевого значения лимита
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getPopularRooms_ShouldHandleZeroLimit() {
        // Arrange
        List<RoomPopularityDto> emptyList = Collections.emptyList();
        Integer zeroLimit = 0;

        when(hotelStatisticsService.getPopularRooms(eq(hotelId), eq(zeroLimit)))
                .thenReturn(emptyList);

        // Act
        ResponseEntity<List<RoomPopularityDto>> response = hotelStatisticsController
                .getPopularRooms(hotelId, zeroLimit);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(hotelStatisticsService, times(1))
                .getPopularRooms(hotelId, zeroLimit);
    }
}