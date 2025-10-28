package com.hotelbooking.hotel.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Статистика по типу номера")
public class RoomTypeStatistics {

    @Schema(description = "Тип номера", example = "DELUXE")
    private String roomType;

    @Schema(description = "Количество номеров этого типа", example = "20")
    private Integer roomCount;

    @Schema(description = "Коэффициент загрузки (%)", example = "85.5")
    private Double occupancyRate;

    @Schema(description = "Общий доход по типу", example = "75000.0")
    private Double totalRevenue;

    @Schema(description = "Среднее количество бронирований", example = "15.3")
    private Double averageBookings;

    @Schema(description = "Доход на номер", example = "3750.0")
    private Double revenuePerRoom;
}