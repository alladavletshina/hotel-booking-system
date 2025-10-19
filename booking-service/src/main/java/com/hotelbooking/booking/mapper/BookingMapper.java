package com.hotelbooking.booking.mapper;

import com.hotelbooking.booking.dto.BookingDto;
import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.User;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingDto toDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setUsername(booking.getUser().getUsername());
        dto.setRoomId(booking.getRoomId());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setStatus(booking.getStatus());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        dto.setCorrelationId(booking.getCorrelationId());

        return dto;
    }

    public Booking toEntity(BookingRequest request) {
        if (request == null) {
            return null;
        }

        Booking booking = new Booking();

        // Создаем минимального пользователя только с ID
        User user = new User();
        user.setId(request.getUserId());
        booking.setUser(user);

        booking.setRoomId(request.getRoomId());
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setCorrelationId(request.getCorrelationId());

        return booking;
    }
}