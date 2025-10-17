package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.HotelDto;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.mapper.HotelMapper;
import com.hotelbooking.hotel.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final HotelMapper hotelMapper;  // Инжектим маппер как бин

    @GetMapping
    public ResponseEntity<List<HotelDto>> getAllHotels() {
        List<Hotel> hotels = hotelService.findAll();
        List<HotelDto> hotelDtos = hotels.stream()
                .map(hotelMapper::toDto)  // Используем метод маппера напрямую
                .collect(Collectors.toList());
        return ResponseEntity.ok(hotelDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelDto> getHotel(@PathVariable Long id) {
        Hotel hotel = hotelService.findById(id);
        return ResponseEntity.ok(hotelMapper.toDto(hotel));  // Используем метод маппера напрямую
    }

    @PostMapping
    public ResponseEntity<HotelDto> createHotel(@RequestBody HotelDto hotelDto) {
        Hotel hotel = hotelMapper.toEntity(hotelDto);  // Используем метод маппера напрямую
        Hotel created = hotelService.save(hotel);
        return ResponseEntity.ok(hotelMapper.toDto(created));  // Используем метод маппера напрямую
    }

    @PutMapping("/{id}")
    public ResponseEntity<HotelDto> updateHotel(@PathVariable Long id, @RequestBody HotelDto hotelDto) {
        Hotel hotel = hotelMapper.toEntity(hotelDto);  // Используем метод маппера напрямую
        Hotel updated = hotelService.update(id, hotel);
        return ResponseEntity.ok(hotelMapper.toDto(updated));  // Используем метод маппера напрямую
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        hotelService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}