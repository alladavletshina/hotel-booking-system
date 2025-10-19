package com.hotelbooking.booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "Booking Service is healthy!";
    }

    @GetMapping("/")
    public String home() {
        return "Welcome to Booking Service!";
    }
}