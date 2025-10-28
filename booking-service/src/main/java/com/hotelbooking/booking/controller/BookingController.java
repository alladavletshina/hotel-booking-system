package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.client.dto.RoomRecommendation;
import com.hotelbooking.booking.dto.BookingDto;
import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.mapper.BookingMapper;
import com.hotelbooking.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/bookings")
@Tag(name = "Bookings", description = "API для управления бронированиями")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @Operation(summary = "Создать бронирование")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingDto> createBooking(@RequestBody BookingRequest request) {
        String correlationId = request.getCorrelationId() != null ?
                request.getCorrelationId() : UUID.randomUUID().toString();

        Booking booking = bookingMapper.toEntity(request);
        Booking createdBooking = bookingService.createBooking(booking, correlationId);

        return ResponseEntity.ok(bookingMapper.toDto(createdBooking));
    }

    @Operation(summary = "Получить бронирования пользователя")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<BookingDto>> getUserBookings(@PathVariable Long userId) {
        List<Booking> bookings = bookingService.getUserBookings(userId);
        List<BookingDto> bookingDtos = bookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(bookingDtos);
    }

    @Operation(summary = "Получить мои бронирования")
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BookingDto>> getMyBookings() {
        List<Booking> bookings = bookingService.getCurrentUserBookings();
        List<BookingDto> bookingDtos = bookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(bookingDtos);
    }

    @Operation(summary = "Отменить бронирование")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Получить все бронирования (ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingDto>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        List<BookingDto> bookingDtos = bookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(bookingDtos);
    }

    @Operation(summary = "Получить рекомендованные номера на даты")
    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<RoomRecommendation>> getRecommendedRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /bookings/recommendations - Getting recommended rooms from {} to {}", startDate, endDate);
        List<RoomRecommendation> recommendations = bookingService.getRecommendedRooms(startDate, endDate);
        return ResponseEntity.ok(recommendations);
    }
}