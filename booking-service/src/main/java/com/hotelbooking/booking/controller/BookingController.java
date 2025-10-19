package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.dto.BookingDto;
import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.mapper.BookingMapper;
import com.hotelbooking.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(@RequestBody BookingRequest bookingRequest,
                                                    @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("Creating booking for user {}, room {}", bookingRequest.getUserId(), bookingRequest.getRoomId());

        Booking booking = bookingMapper.toEntity(bookingRequest);
        Booking createdBooking = bookingService.createBooking(booking, correlationId);
        BookingDto bookingDto = bookingMapper.toDto(createdBooking);

        return ResponseEntity.ok(bookingDto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingDto>> getUserBookings(@PathVariable Long userId) {
        List<Booking> bookings = bookingService.getUserBookings(userId);
        List<BookingDto> bookingDtos = bookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(bookingDtos);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDto> getBooking(@PathVariable Long bookingId) {
        Booking booking = bookingService.getBookingById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        BookingDto bookingDto = bookingMapper.toDto(booking);

        return ResponseEntity.ok(bookingDto);
    }

    @GetMapping
    public ResponseEntity<List<BookingDto>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        List<BookingDto> bookingDtos = bookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(bookingDtos);
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingDto> cancelBooking(@PathVariable Long bookingId) {
        Booking cancelledBooking = bookingService.cancelBooking(bookingId);
        BookingDto bookingDto = bookingMapper.toDto(cancelledBooking);

        return ResponseEntity.ok(bookingDto);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<BookingDto>> getBookingsByStatus(@PathVariable BookingStatus status) {
        List<Booking> bookings = bookingService.getBookingsByStatus(status);
        List<BookingDto> bookingDtos = bookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(bookingDtos);
    }

    @GetMapping("/room/{roomId}/availability")
    public ResponseEntity<Boolean> checkRoomAvailability(@PathVariable Long roomId,
                                                         @RequestParam LocalDate startDate,
                                                         @RequestParam LocalDate endDate) {
        boolean available = bookingService.isRoomAvailableForDates(roomId, startDate, endDate);
        return ResponseEntity.ok(available);
    }
}