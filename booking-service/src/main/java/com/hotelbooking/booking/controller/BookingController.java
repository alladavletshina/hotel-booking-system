package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.dto.BookingDto;
import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.mapper.BookingMapper;
import com.hotelbooking.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Управление бронированиями", description = "API для управления бронированиями отелей")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @Operation(summary = "Создание бронирования", description = "Создание нового бронирования отеля")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Бронирование успешно создано",
                    content = @Content(schema = @Schema(implementation = BookingDto.class))),
            @ApiResponse(responseCode = "400", description = "Неверные данные бронирования"),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ")
    })
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BookingDto> createBooking(
            @RequestBody BookingRequest bookingRequest,
            @Parameter(description = "Correlation ID для отслеживания запроса")
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Creating booking for room {}", bookingRequest.getRoomId());

        Booking booking = bookingMapper.toEntity(bookingRequest);
        Booking createdBooking = bookingService.createBooking(booking, correlationId);
        BookingDto bookingDto = bookingMapper.toDto(createdBooking);

        return ResponseEntity.ok(bookingDto);
    }

    @Operation(summary = "Получение бронирований текущего пользователя", description = "Получение всех бронирований для текущего аутентифицированного пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Бронирования успешно получены"),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ")
    })
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<BookingDto>> getMyBookings() {
        List<Booking> bookings = bookingService.getCurrentUserBookings();
        List<BookingDto> bookingDtos = bookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(bookingDtos);
    }

    @Operation(summary = "Получение бронирований пользователя", description = "Получение всех бронирований для конкретного пользователя (только для ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Бронирования успешно получены"),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingDto>> getUserBookings(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable Long userId) {
        List<Booking> bookings = bookingService.getUserBookings(userId);
        List<BookingDto> bookingDtos = bookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(bookingDtos);
    }

    @Operation(summary = "Получение бронирования по ID", description = "Получение деталей конкретного бронирования")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Бронирование успешно получено",
                    content = @Content(schema = @Schema(implementation = BookingDto.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
            @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    })
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BookingDto> getBooking(
            @Parameter(description = "ID бронирования", required = true)
            @PathVariable Long bookingId) {
        Booking booking = bookingService.getBookingById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        BookingDto bookingDto = bookingMapper.toDto(booking);

        return ResponseEntity.ok(bookingDto);
    }

    @Operation(summary = "Отмена бронирования", description = "Отмена существующего бронирования")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Бронирование успешно отменено"),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
            @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    })
    @DeleteMapping("/{bookingId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> cancelBooking(
            @Parameter(description = "ID бронирования", required = true)
            @PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok().build();
    }
}