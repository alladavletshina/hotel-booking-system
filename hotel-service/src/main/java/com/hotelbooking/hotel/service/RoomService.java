package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelService hotelService;

    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
    }

    public List<Room> findAvailableRooms() {
        return roomRepository.findByAvailableTrue();
    }

    public List<Room> findRecommendedRooms() {
        return roomRepository.findAvailableRoomsOrderByTimesBooked();
    }

    public Room save(Room room) {
        // Убедимся, что timesBooked не null
        if (room.getTimesBooked() == null) {
            room.setTimesBooked(0);
        }

        // Если у комнаты указан hotelId, устанавливаем связь
        if (room.getHotel() != null && room.getHotel().getId() != null) {
            Hotel hotel = hotelService.findById(room.getHotel().getId());
            room.setHotel(hotel);
        }
        return roomRepository.save(room);
    }

    public Room update(Long id, Room roomDetails) {
        Room room = findById(id);
        room.setNumber(roomDetails.getNumber());
        room.setType(roomDetails.getType());
        room.setPrice(roomDetails.getPrice());
        room.setAvailable(roomDetails.getAvailable());
        return roomRepository.save(room);
    }

    public void deleteById(Long id) {
        roomRepository.deleteById(id);
    }

    @Transactional
    public boolean confirmAvailability(Long roomId) {
        try {
            Room room = findById(roomId);

            // Защита от NullPointerException
            if (room.getTimesBooked() == null) {
                room.setTimesBooked(0);
            }

            if (room.getAvailable()) {
                room.setTimesBooked(room.getTimesBooked() + 1);
                roomRepository.save(room);
                log.info("Room {} availability confirmed, timesBooked: {}", roomId, room.getTimesBooked());
                return true;
            }
            log.info("Room {} is not available", roomId);
            return false;
        } catch (Exception e) {
            log.error("Error confirming availability for room: {}", roomId, e);
            throw new RuntimeException("Error confirming availability for room: " + roomId, e);
        }
    }

    @Transactional
    public void releaseRoom(Long roomId) {
        try {
            Room room = findById(roomId);

            // Защита от NullPointerException
            if (room.getTimesBooked() == null) {
                room.setTimesBooked(0);
            }

            if (room.getTimesBooked() > 0) {
                room.setTimesBooked(room.getTimesBooked() - 1);
                roomRepository.save(room);
                log.info("Room {} released, timesBooked: {}", roomId, room.getTimesBooked());
            } else {
                log.info("Room {} timesBooked is already 0", roomId);
            }
        } catch (Exception e) {
            log.error("Error releasing room: {}", roomId, e);
            throw new RuntimeException("Error releasing room: " + roomId, e);
        }
    }
}