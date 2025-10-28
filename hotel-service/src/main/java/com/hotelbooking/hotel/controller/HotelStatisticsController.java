package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.statistics.HotelStatisticsDto;
import com.hotelbooking.hotel.dto.statistics.RoomPopularityDto;
import com.hotelbooking.hotel.service.HotelStatisticsService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/hotel")
@Tag(name = "Hotel Statistics", description = "API для расширенной статистики отелей")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class HotelStatisticsController {

    private final HotelStatisticsService hotelStatisticsService;

    @Operation(
            summary = "Получить статистику по отелю",
            description = "Возвращает расширенную статистику по отелю за указанный период"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика получена"),
            @ApiResponse(responseCode = "404", description = "Отель не найден")
    })
    @GetMapping("/{hotelId}/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HotelStatisticsDto> getHotelStatistics(
            @Parameter(description = "ID отеля") @PathVariable Long hotelId,
            @Parameter(description = "Начальная дата (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Конечная дата (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /hotels/{}/statistics - Getting statistics from {} to {}", hotelId, startDate, endDate);
        HotelStatisticsDto statistics = hotelStatisticsService.getHotelStatistics(hotelId, startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    @Operation(
            summary = "Сравнительная статистика отелей",
            description = "Возвращает сравнительную статистику по всем отелям"
    )
    @GetMapping("/statistics/comparison")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HotelStatisticsDto>> getHotelsComparison(
            @Parameter(description = "Начальная дата (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Конечная дата (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /hotels/statistics/comparison - Getting comparison from {} to {}", startDate, endDate);
        List<HotelStatisticsDto> comparison = hotelStatisticsService.getHotelsComparison(startDate, endDate);
        return ResponseEntity.ok(comparison);
    }

    @Operation(
            summary = "Статистика загруженности по дням",
            description = "Возвращает детальную статистику загруженности по дням для отеля"
    )
    @GetMapping("/{hotelId}/occupancy-daily")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<LocalDate, Double>> getDailyOccupancy(
            @Parameter(description = "ID отеля") @PathVariable Long hotelId,
            @Parameter(description = "Начальная дата (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Конечная дата (формат: YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /hotels/{}/occupancy-daily - Daily occupancy from {} to {}", hotelId, startDate, endDate);
        Map<LocalDate, Double> dailyOccupancy = hotelStatisticsService.getDailyOccupancy(hotelId, startDate, endDate);
        return ResponseEntity.ok(dailyOccupancy);
    }

    @Operation(
            summary = "Топ популярных номеров",
            description = "Возвращает топ самых популярных номеров отеля"
    )
    @GetMapping("/{hotelId}/popular-rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoomPopularityDto>> getPopularRooms(
            @Parameter(description = "ID отеля") @PathVariable Long hotelId,
            @Parameter(description = "Лимит результатов") @RequestParam(defaultValue = "10") Integer limit) {

        log.info("GET /hotels/{}/popular-rooms - Top {} popular rooms", hotelId, limit);
        List<RoomPopularityDto> popularRooms = hotelStatisticsService.getPopularRooms(hotelId, limit);
        return ResponseEntity.ok(popularRooms);
    }
}