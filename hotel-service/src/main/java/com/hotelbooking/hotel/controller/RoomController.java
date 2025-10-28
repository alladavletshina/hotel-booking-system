package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.*;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.mapper.RoomMapper;
import com.hotelbooking.hotel.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/rooms")
@Tag(name = "Rooms", description = "API для управления номерами")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @Operation(summary = "Получить доступные номера", description = "Возвращает список всех свободных номеров")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный запрос")
    })
    @GetMapping()
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<RoomDto>> getAvailableRooms() {
        log.info("GET /rooms - Getting available rooms");
        List<Room> rooms = roomService.findAvailableRooms();
        List<RoomDto> roomDtos = rooms.stream()
                .map(roomMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomDtos);
    }

    @Operation(summary = "Получить номер по ID", description = "Возвращает номер по указанному ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RoomDto> getRoom(@Parameter(description = "ID номера") @PathVariable long id){
        Room room = roomService.findById(id);
        RoomDto roomDto = roomMapper.toDto(room);
        return ResponseEntity.ok(roomDto);
    }

    @Operation(summary = "Получить номера по отелю", description = "Возвращает все номера указанного отеля")
    @GetMapping("/hotel/{hotelId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<RoomDto>> getRoomsByHotel(@Parameter(description = "ID отеля") @PathVariable long hotelId){
        List<Room> rooms = roomService.findRoomsByHotelId(hotelId);
        List<RoomDto> roomDtos = rooms.stream()
                .map(roomMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomDtos);
    }

    @Operation(summary = "Удалить номер", description = "Удаляет номер по ID (только для ADMIN)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@Parameter(description = "ID номера") @PathVariable long id){
        roomService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Получить рекомендованные номера", description = "Возвращает свободные номера, отсортированные по популярности")
    @GetMapping("/recommend")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<RoomDto>> getRecommendedRooms() {
        List<Room> rooms = roomService.findRecommendedRooms();
        List<RoomDto> roomDtos = rooms.stream()
                .map(roomMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomDtos);
    }

    @Operation(summary = "Создать номер", description = "Создает новый номер в отеле (только для ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomDto> createRoom(@Parameter(description = "Данные номера") @RequestBody RoomDto roomDto) {
        Room room = roomMapper.toEntity(roomDto);
        Room created = roomService.save(room);
        return ResponseEntity.ok(roomMapper.toDto(created));
    }

    @Operation(summary = "Проверить доступность номера на даты",
            description = "Проверяет, доступен ли номер для бронирования на указанные даты")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Проверка выполнена успешно"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры дат")
    })
    @GetMapping("/{id}/availability")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> checkAvailability(
            @Parameter(description = "ID номера") @PathVariable Long id,
            @Parameter(description = "Дата заезда (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Дата выезда (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /rooms/{}/availability - Checking availability from {} to {}", id, startDate, endDate);
        boolean available = roomService.isRoomAvailable(id, startDate, endDate);
        return ResponseEntity.ok(available);
    }

    @Operation(summary = "Найти доступные номера на даты",
            description = "Возвращает номера, доступные для бронирования на указанные даты")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список номеров получен"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры дат")
    })
    @GetMapping("/available")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<RoomDto>> getAvailableRoomsForDates(
            @Parameter(description = "Дата заезда (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Дата выезда (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /rooms/available - Finding available rooms from {} to {}", startDate, endDate);
        List<Room> rooms = roomService.findAvailableRooms(startDate, endDate);
        List<RoomDto> roomDtos = rooms.stream()
                .map(roomMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomDtos);
    }

    @Operation(summary = "Получить рекомендованные номера на даты",
            description = "Возвращает рекомендованные номера на указанные даты, отсортированные по популярности")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список номеров получен"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры дат")
    })
    @GetMapping("/recommend/date")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<RoomDto>> getRecommendedRoomsForDates(
            @Parameter(description = "Дата заезда (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Дата выезда (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /rooms/recommend/date - Finding recommended rooms from {} to {}", startDate, endDate);
        List<Room> rooms = roomService.findRecommendedRooms(startDate, endDate);
        List<RoomDto> roomDtos = rooms.stream()
                .map(roomMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomDtos);
    }

    @Operation(summary = "Подтвердить доступность на даты",
            description = "Подтверждает доступность номера на указанные даты с временной блокировкой (INTERNAL)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Доступность проверена"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса")
    })
    @PostMapping("/{id}/confirm-availability-with-dates")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Boolean> confirmAvailabilityWithDates(
            @Parameter(description = "ID номера") @PathVariable Long id,
            @RequestBody AvailabilityRequest request) {

        log.info("POST /rooms/{}/confirm-availability-with-dates - Confirming availability for dates {} to {} (booking: {})",
                id, request.getStartDate(), request.getEndDate(), request.getBookingId());

        boolean available = roomService.confirmAvailability(
                id, request.getStartDate(), request.getEndDate(), request.getBookingId());
        return ResponseEntity.ok(available);
    }

    @Operation(summary = "Подтвердить доступность",
            description = "Подтверждает доступность номера на даты (INTERNAL) - устаревшая версия")
    @PostMapping("/{id}/confirm-availability")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Boolean> confirmAvailability(@Parameter(description = "ID номера") @PathVariable Long id) {
        log.warn("Using deprecated confirmAvailability endpoint without dates");
        boolean available = roomService.confirmAvailability(id);
        return ResponseEntity.ok(available);
    }

    @Operation(summary = "Снять блокировку по бронированию",
            description = "Снимает временную блокировку номера для конкретного бронирования (INTERNAL)")
    @PostMapping("/{id}/release-booking")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Void> releaseRoomWithBooking(
            @Parameter(description = "ID номера") @PathVariable Long id,
            @RequestBody ReleaseRequest request) {

        log.info("POST /rooms/{}/release-booking - Releasing room for booking {}", id, request.getBookingId());
        roomService.releaseRoom(id, request.getBookingId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Снять блокировку",
            description = "Снимает временную блокировку номера (INTERNAL) - устаревшая версия")
    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Void> releaseRoom(@Parameter(description = "ID номера") @PathVariable Long id) {
        log.warn("Using deprecated releaseRoom endpoint without bookingId");
        roomService.releaseRoom(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Подтвердить бронирование",
            description = "Подтверждает бронирование номера (перевод из RESERVED в CONFIRMED) (INTERNAL)")
    @PostMapping("/{id}/confirm-booking")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Void> confirmBooking(
            @Parameter(description = "ID номера") @PathVariable Long id,
            @RequestBody BookingConfirmationRequest request) {

        log.info("POST /rooms/{}/confirm-booking - Confirming booking {}", id, request.getBookingId());
        roomService.confirmBooking(id, request.getBookingId());
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Отменить бронирование",
            description = "Отменяет бронирование номера (INTERNAL)")
    @PostMapping("/{id}/cancel-booking")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Void> cancelBooking(
            @Parameter(description = "ID номера") @PathVariable Long id,
            @RequestBody BookingConfirmationRequest request) {

        log.info("POST /rooms/{}/cancel-booking - Cancelling booking {}", id, request.getBookingId());
        roomService.cancelBooking(id, request.getBookingId());
        return ResponseEntity.ok().build();
    }

}