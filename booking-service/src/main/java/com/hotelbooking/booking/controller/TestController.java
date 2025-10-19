package com.hotelbooking.booking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/status")
    public String status() {
        return "Booking Service is running!";
    }

    @GetMapping
    public String all() {
        return "Booking Service is running!-TEST";
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Booking Service!";
    }
}