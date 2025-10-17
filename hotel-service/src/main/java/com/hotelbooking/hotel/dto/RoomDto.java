package com.hotelbooking.hotel.dto;

import lombok.Data;

@Data
public class RoomDto {
    private Long id;
    private String number;
    private String type;
    private Double price;
    private Boolean available;
    private Integer timesBooked;
    private Long hotelId;
}