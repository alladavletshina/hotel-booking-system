package com.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/test")
@Tag(name = "Тестовые endpoints", description = "API для проверки работоспособности сервиса аутентификации")
public class TestController {

    @Operation(summary = "Проверка статуса сервиса", description = "Публичный endpoint для проверки работы сервиса аутентификации")
    @GetMapping("/status")
    public String status() {
        return "Auth Service is running on port 8081!";
    }

    @Operation(summary = "Приветственное сообщение", description = "Публичный endpoint для приветственного сообщения")
    @GetMapping("/hello")
    public String hello() {
        return "Hello from Auth Service!";
    }
}