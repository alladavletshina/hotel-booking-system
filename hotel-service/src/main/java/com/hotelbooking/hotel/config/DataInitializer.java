package com.hotelbooking.hotel.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initTestData() {
        return args -> {
            log.info("Hotel Service: Data initialization completed");
            // Здесь можно добавить тестовые отели и номера, если нужно
            log.info("Hotel Service is ready to accept requests");
        };
    }
}