package com.hotelbooking.booking.client;

import com.hotelbooking.booking.client.dto.AvailabilityRequest;
import com.hotelbooking.booking.client.dto.ReleaseRequest;
import com.hotelbooking.booking.client.dto.BookingConfirmationRequest;
import com.hotelbooking.booking.client.dto.RoomRecommendation;
import com.hotelbooking.booking.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@FeignClient(
        name = "hotel-service",
        url = "http://localhost:8082",
        configuration = FeignConfig.class
)
public interface HotelServiceClient {

    @PostMapping("/rooms/{roomId}/confirm-availability")
    Boolean confirmAvailability(@PathVariable("roomId") Long roomId,
                                @RequestBody AvailabilityRequest request);

    @PostMapping("/rooms/{roomId}/release")
    void releaseRoom(@PathVariable("roomId") Long roomId,
                     @RequestBody ReleaseRequest request);

    @PostMapping("/rooms/{roomId}/confirm-availability-with-dates")
    Boolean confirmAvailabilityWithDates(@PathVariable("roomId") Long roomId,
                                         @RequestBody AvailabilityRequest request);

    @PostMapping("/rooms/{roomId}/confirm-booking")
    void confirmBooking(@PathVariable("roomId") Long roomId,
                        @RequestBody BookingConfirmationRequest request);

    @PostMapping("/rooms/{roomId}/cancel-booking")
    void cancelBooking(@PathVariable("roomId") Long roomId,
                       @RequestBody BookingConfirmationRequest request);

    @GetMapping("/rooms/recommend/date")
    List<RoomRecommendation> getRecommendedRooms(
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate);
}