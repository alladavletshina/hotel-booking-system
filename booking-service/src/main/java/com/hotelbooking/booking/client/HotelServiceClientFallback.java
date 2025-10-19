package com.hotelbooking.booking.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HotelServiceClientFallback implements HotelServiceClient {

    @Override
    public Boolean confirmAvailability(Long roomId) {
        log.error("Fallback: Hotel Service unavailable. Cannot confirm availability for room {}", roomId);
        return false; // При недоступности сервиса считаем комнату недоступной
    }

    @Override
    public void releaseRoom(Long roomId) {
        log.error("Fallback: Hotel Service unavailable. Cannot release room {}", roomId);
        // В fallback не можем освободить комнату, просто логируем
    }
}