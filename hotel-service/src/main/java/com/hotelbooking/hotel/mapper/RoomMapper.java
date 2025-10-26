package com.hotelbooking.hotel.mapper;

import com.hotelbooking.hotel.dto.RoomDto;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class RoomMapper {

    private final HotelRepository hotelRepository; // ← ДОБАВЬТЕ ЭТО

    public RoomDto toDto(Room room) {
        if (room == null) {
            return null;
        }

        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setNumber(room.getNumber());
        dto.setType(room.getType());
        dto.setPrice(room.getPrice());
        dto.setAvailable(room.getAvailable());

        // Защита от null
        dto.setTimesBooked(room.getTimesBooked() != null ? room.getTimesBooked() : 0);

        if (room.getHotel() != null) {
            dto.setHotelId(room.getHotel().getId());
        }

        return dto;
    }

    public Room toEntity(RoomDto roomDto) {
        if (roomDto == null) {
            return null;
        }

        Room room = new Room();
        room.setId(roomDto.getId());
        room.setNumber(roomDto.getNumber());
        room.setType(roomDto.getType());
        room.setPrice(roomDto.getPrice());
        room.setAvailable(roomDto.getAvailable());

        // Защита от null
        room.setTimesBooked(roomDto.getTimesBooked() != null ? roomDto.getTimesBooked() : 0);

        // ДОБАВЬТЕ ЭТУ ЛОГИКУ ДЛЯ УСТАНОВКИ HOTEL:
        if (roomDto.getHotelId() != null) {
            Hotel hotel = hotelRepository.findById(roomDto.getHotelId())
                    .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + roomDto.getHotelId()));
            room.setHotel(hotel);
        }

        return room;
    }
}