package com.hotelbooking.booking.client;

import com.hotelbooking.booking.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "hotel-service",
        url = "http://localhost:8082",
        configuration = FeignConfig.class  // ЯВНО УКАЗЫВАЕМ КОНФИГУРАЦИЮ
)
public interface HotelServiceClient {

    @PostMapping("/rooms/{roomId}/confirm-availability")
    Boolean confirmAvailability(@PathVariable("roomId") Long roomId);

    @PostMapping("/rooms/{roomId}/release")
    void releaseRoom(@PathVariable("roomId") Long roomId);
}