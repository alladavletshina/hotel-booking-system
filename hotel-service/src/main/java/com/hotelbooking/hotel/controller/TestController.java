package com.hotelbooking.hotel.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hotel/test")
public class TestController {

    @GetMapping("/hello")
    public String test() {
        return "Test endpoint is working! Hotel Service is alive!";
    }

    @GetMapping("/public")
    public String publicEndpoint() {
        return "This is a public endpoint - no authentication required!";
    }
}
