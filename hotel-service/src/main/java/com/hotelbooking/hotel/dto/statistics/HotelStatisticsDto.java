package com.hotelbooking.hotel.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
@Schema(description = "Расширенная статистика по отелю")
public class HotelStatisticsDto {

    @Schema(description = "ID отеля", example = "1")
    private Long hotelId;

    @Schema(description = "Название отеля", example = "Grand Plaza Hotel")
    private String hotelName;

    @Schema(description = "Общее количество номеров", example = "50")
    private Integer totalRooms;

    @Schema(description = "Количество доступных номеров", example = "35")
    private Integer availableRooms;

    @Schema(description = "Коэффициент загрузки отеля (%)", example = "72.5")
    private Double occupancyRate;

    @Schema(description = "Средняя загрузка по дням")
    private Map<LocalDate, Double> dailyOccupancy;

    @Schema(description = "Статистика по типам номеров")
    private Map<String, RoomTypeStatistics> roomTypeStats;

    @Schema(description = "Самый популярный номер")
    private RoomPopularityDto mostPopularRoom;

    @Schema(description = "Наименее популярный номер")
    private RoomPopularityDto leastPopularRoom;

    @Schema(description = "Общий доход", example = "125000.0")
    private Double totalRevenue;

    @Schema(description = "Средний доход за номер", example = "2500.0")
    private Double averageRevenuePerRoom;

    @Schema(description = "Период анализа")
    private DateRange dateRange;
}