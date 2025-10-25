package com.hotelbooking.booking.mapper;

import com.hotelbooking.booking.dto.BookingDto;
import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.entity.Booking;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingDto toDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUserId());
        dto.setUsername(booking.getUsername());
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

        // Получаем информацию о пользователе из JWT токена
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new RuntimeException("User not authenticated");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String username = jwt.getClaimAsString("sub"); // username из токена

        // Получаем userId из claims (может быть как Long, так и String)
        Long userId = extractUserIdFromJwt(jwt);

        Booking booking = new Booking();
        booking.setUserId(userId != null ? userId : generateUserIdFromUsername(username));
        booking.setUsername(username);
        booking.setRoomId(request.getRoomId());
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setCorrelationId(request.getCorrelationId());

        return booking;
    }

    private Long extractUserIdFromJwt(Jwt jwt) {
        try {
            // Пробуем получить userId как Long
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim instanceof Long) {
                return (Long) userIdClaim;
            } else if (userIdClaim instanceof Integer) {
                return ((Integer) userIdClaim).longValue();
            } else if (userIdClaim instanceof String) {
                return Long.parseLong((String) userIdClaim);
            }
        } catch (Exception e) {
            // Если userId нет в токене или не может быть преобразован
            return null;
        }
        return null;
    }

    private Long generateUserIdFromUsername(String username) {
        // Генерируем детерминированный ID из username для демонстрации
        return (long) Math.abs(username.hashCode());
    }
}