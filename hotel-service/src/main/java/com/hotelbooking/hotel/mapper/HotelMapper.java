package com.hotelbooking.hotel.mapper;

import com.hotelbooking.hotel.dto.HotelDto;
import com.hotelbooking.hotel.entity.Hotel;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

    public HotelDto toDto(Hotel hotel) {
        if (hotel == null) {
            return null;
        }

        HotelDto dto = new HotelDto();
        dto.setId(hotel.getId());
        dto.setName(hotel.getName());
        dto.setAddress(hotel.getAddress());
        dto.setDescription(hotel.getDescription());
        return dto;
    }

    public Hotel toEntity(HotelDto hotelDto) {
        if (hotelDto == null) {
            return null;
        }

        Hotel hotel = new Hotel();
        hotel.setId(hotelDto.getId());
        hotel.setName(hotelDto.getName());
        hotel.setAddress(hotelDto.getAddress());
        hotel.setDescription(hotelDto.getDescription());
        return hotel;
    }
}
