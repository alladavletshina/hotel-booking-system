package com.hotelbooking.hotel.dto;

import lombok.Data;

@Data
public class HotelDto {
    private Long id;
    private String name;
    private String address;
    private String description;
}