package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.HotelDto;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.mapper.HotelMapper;
import com.hotelbooking.hotel.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/hotels")
@Tag(name = "Hotels", description = "API для управления отелями")
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final HotelMapper hotelMapper;

    @Operation(summary = "Получить все отели", description = "Возвращает список всех отелей")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный запрос"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<HotelDto>> getAllHotels() {
        log.info("GET /hotels - Getting all hotels");
        List<Hotel> hotels = hotelService.findAll();
        List<HotelDto> hotelDtos = hotels.stream()
                .map(hotelMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(hotelDtos);
    }

    @Operation(summary = "Получить отель по ID", description = "Возвращает отель по указанному ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Отель найден"),
            @ApiResponse(responseCode = "404", description = "Отель не найден")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<HotelDto> getHotel(@Parameter(description = "ID отеля") @PathVariable Long id) {
        Hotel hotel = hotelService.findById(id);
        return ResponseEntity.ok(hotelMapper.toDto(hotel));
    }

    @Operation(summary = "Создать отель", description = "Создает новый отель (только для ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Отель создан"),
            @ApiResponse(responseCode = "403", description = "Нет прав ADMIN")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HotelDto> createHotel(@Parameter(description = "Данные отеля") @RequestBody HotelDto hotelDto) {
        Hotel hotel = hotelMapper.toEntity(hotelDto);
        Hotel created = hotelService.save(hotel);
        return ResponseEntity.ok(hotelMapper.toDto(created));
    }

    @Operation(summary = "Обновить отель", description = "Обновляет данные отеля (только для ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HotelDto> updateHotel(
            @Parameter(description = "ID отеля") @PathVariable Long id,
            @Parameter(description = "Обновленные данные отеля") @RequestBody HotelDto hotelDto) {
        Hotel hotel = hotelMapper.toEntity(hotelDto);
        Hotel updated = hotelService.update(id, hotel);
        return ResponseEntity.ok(hotelMapper.toDto(updated));
    }

    @Operation(summary = "Удалить отель", description = "Удаляет отель по ID (только для ADMIN)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHotel(@Parameter(description = "ID отеля")  @PathVariable Long id) {
        hotelService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}