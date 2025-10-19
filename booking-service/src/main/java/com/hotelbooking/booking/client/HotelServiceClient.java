package com.hotelbooking.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "hotel-service", fallback = HotelServiceClientFallback.class)
public interface HotelServiceClient {

    @PostMapping("/rooms/{id}/confirm-availability")
    Boolean confirmAvailability(@PathVariable("id") Long roomId);

    @PostMapping("/rooms/{id}/release")
    void releaseRoom(@PathVariable("id") Long roomId);
}