package com.hotelbooking.hotel.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "Hotel Service with Eureka is working!";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}