package com.hotelbooking.booking.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequest {
    private Long userId;
    private Long roomId;        // Игнорируется при autoSelect = true
    private LocalDate startDate;
    private LocalDate endDate;
    private String correlationId;
    private Boolean autoSelect = false;
}