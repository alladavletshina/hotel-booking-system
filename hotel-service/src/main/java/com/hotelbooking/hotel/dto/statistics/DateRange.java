package com.hotelbooking.hotel.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Диапазон дат для анализа")
public class DateRange {

    @Schema(description = "Начальная дата", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "Конечная дата", example = "2024-01-31")
    private LocalDate endDate;

    @Schema(description = "Количество дней в периоде", example = "31")
    private Long daysCount;
}