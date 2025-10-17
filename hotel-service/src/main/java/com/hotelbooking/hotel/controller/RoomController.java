package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.RoomDto;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.mapper.RoomMapper;
import com.hotelbooking.hotel.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;  // Инжектим маппер как бин

    @GetMapping()
    public ResponseEntity<List<RoomDto>> getAvailableRooms() {
        List<Room> rooms = roomService.findAvailableRooms();
        List<RoomDto> roomDtos = rooms.stream()
                .map(roomMapper::toDto)  // Используем метод маппера напрямую
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomDtos);
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<RoomDto>> getRecommendedRooms() {
        List<Room> rooms = roomService.findRecommendedRooms();
        List<RoomDto> roomDtos = rooms.stream()
                .map(roomMapper::toDto)  // Используем метод маппера напрямую
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomDtos);
    }

    @PostMapping
    public ResponseEntity<RoomDto> createRoom(@RequestBody RoomDto roomDto) {
        Room room = roomMapper.toEntity(roomDto);  // Используем метод маппера напрямую
        Room created = roomService.save(room);
        return ResponseEntity.ok(roomMapper.toDto(created));  // Используем метод маппера напрямую
    }

    @PostMapping("/{id}/confirm-availability")
    public ResponseEntity<Boolean> confirmAvailability(@PathVariable Long id) {
        boolean available = roomService.confirmAvailability(id);
        return ResponseEntity.ok(available);
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseRoom(@PathVariable Long id) {
        roomService.releaseRoom(id);
        return ResponseEntity.ok().build();
    }
}