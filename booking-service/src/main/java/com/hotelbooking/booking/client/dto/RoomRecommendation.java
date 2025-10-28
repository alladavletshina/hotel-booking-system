package com.hotelbooking.booking.client.dto;

import lombok.Data;

@Data
public class RoomRecommendation {
    private Long id;
    private String number;
    private String type;
    private Double price;
    private Boolean available;
    private Integer timesBooked;
    private Long hotelId;
    private String hotelName;
    private String hotelAddress;
}