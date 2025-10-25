package com.hotelbooking.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/booking/test")
@RequiredArgsConstructor
@Tag(name = "Тестовые endpoints", description = "API для проверки работоспособности сервиса")
public class TestController {

    @Operation(summary = "Проверка статуса сервиса", description = "Возвращает статус работы сервиса бронирования")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сервис работает корректно")
    })
    @GetMapping("/status")
    public String status() {
        return "Booking Service is running!";
    }

    @Operation(summary = "Основной тестовый endpoint", description = "Основной endpoint для проверки работы сервиса")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сервис работает корректно")
    })
    @GetMapping
    public String all() {
        return "Booking Service is running!-TEST";
    }

    @Operation(summary = "Приветственное сообщение", description = "Возвращает приветственное сообщение от сервиса бронирования")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Приветственное сообщение успешно получено")
    })
    @GetMapping("/hello")
    public String hello() {
        return "Hello from Booking Service!";
    }
}