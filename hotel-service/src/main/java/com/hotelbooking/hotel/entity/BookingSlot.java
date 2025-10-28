package com.hotelbooking.hotel.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "booking_slots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "start_date", "end_date"}))
@Data
public class BookingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "status", nullable = false)
    private String status = "RESERVED"; // RESERVED, CONFIRMED, CANCELLED

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
}