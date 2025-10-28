package com.hotelbooking.hotel.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Популярность номера")
public class RoomPopularityDto {

    @Schema(description = "ID номера", example = "101")
    private Long roomId;

    @Schema(description = "Номер комнаты", example = "301")
    private String roomNumber;

    @Schema(description = "Тип номера", example = "SUITE")
    private String roomType;

    @Schema(description = "Цена за ночь", example = "350.0")
    private Double price;

    @Schema(description = "Количество бронирований", example = "25")
    private Integer timesBooked;

    @Schema(description = "Коэффициент загрузки (%)", example = "68.5")
    private Double occupancyRate;

    @Schema(description = "Общий доход", example = "8750.0")
    private Double totalRevenue;

    @Schema(description = "Рейтинг популярности", example = "1")
    private Integer popularityRank;
}