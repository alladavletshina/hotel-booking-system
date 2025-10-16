package com.hotelbooking.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDiscoveryClient
public class HotelServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelServiceApplication.class, args);
    }
}