package com.hotelbooking.hotel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Объект передачи данных для отеля")
public class HotelDto {

    @Schema(description = "ID отеля", example = "1")
    private Long id;

    @Schema(description = "Название отеля", example = "Grand Plaza Hotel")
    private String name;

    @Schema(description = "Адрес отеля", example = "123 Main Street, City Center")
    private String address;

    @Schema(description = "Описание отеля", example = "Luxury 5-star hotel with premium amenities")
    private String description;
}