package com.hotelbooking.hotel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Объект передачи данных для номера")
public class RoomDto {

    @Schema(description = "ID номера", example = "1")
    private Long id;

    @Schema(description = "Номер комнаты", example = "101")
    private String number;

    @Schema(description = "Тип номера", example = "DELUXE")
    private String type;

    @Schema(description = "Цена номера", example = "5000")
    private Double price;

    @Schema(description = "Доступен ли номер", example = "true")
    private Boolean available;

    @Schema(description = "Количество бронирований", example = "5")
    private Integer timesBooked;

    @Schema(description = "ID отеля", example = "1")
    private Long hotelId;
}