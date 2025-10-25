package com.hotelbooking.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "hotel-service", path = "/rooms")
public interface HotelServiceClient {

    @PostMapping("/{roomId}/confirm-availability")
    Boolean confirmAvailability(@PathVariable("roomId") Long roomId);

    @PostMapping("/{roomId}/release")
    void releaseRoom(@PathVariable("roomId") Long roomId);
}