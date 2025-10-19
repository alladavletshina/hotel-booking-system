package com.hotelbooking.booking.dto;

import com.hotelbooking.booking.entity.BookingStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookingDto {
    private Long id;
    private Long userId;
    private String username;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String correlationId;
}