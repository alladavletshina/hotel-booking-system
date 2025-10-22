package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.RoomDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/rooms")
@Tag(name = "Rooms", description = "API для управления номерами")
@RequiredArgsConstructor
@SecurityRequirement(name = "basicAuth")
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

    @Operation(summary = "Подтвердить доступность", description = "Подтверждает доступность номера на даты (INTERNAL)")
    @PostMapping("/{id}/confirm-availability")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Boolean> confirmAvailability(@Parameter(description = "ID номера")  @PathVariable Long id) {
        boolean available = roomService.confirmAvailability(id);
        return ResponseEntity.ok(available);
    }

    @Operation(summary = "Снять блокировку", description = "Снимает временную блокировку номера (INTERNAL)")
    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Void> releaseRoom(@Parameter(description = "ID номера") @PathVariable Long id) {
        roomService.releaseRoom(id);
        return ResponseEntity.ok().build();
    }
}