package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    @Transactional
    public boolean confirmAvailability(Long roomId) {
        try {
            log.info("Confirming availability for room: {}", roomId);

            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isEmpty()) {
                log.error("Room not found: {}", roomId);
                return false;
            }

            Room room = roomOpt.get();

            // Проверяем базовую доступность номера
            if (!room.getAvailable()) {
                log.warn("Room {} is not available for booking", roomId);
                return false;
            }

            // Здесь должна быть логика проверки дат и временная блокировка
            // Для демонстрации просто возвращаем true и увеличиваем счетчик бронирований

            // Увеличиваем счетчик бронирований (для рекомендательной системы)
            room.setTimesBooked(room.getTimesBooked() != null ? room.getTimesBooked() + 1 : 1);
            roomRepository.save(room);

            log.info("Room {} availability confirmed. Times booked: {}", roomId, room.getTimesBooked());
            return true;

        } catch (Exception e) {
            log.error("Error confirming availability for room {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    @Transactional
    public void releaseRoom(Long roomId) {
        try {
            log.info("Releasing room: {}", roomId);

            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                // Снимаем временную блокировку если она была установлена
                // В реальном приложении здесь бы обновлялся статус блокировки
                log.info("Room {} released from temporary lock", roomId);

                // Можно уменьшить счетчик бронирований при отмене
                // room.setTimesBooked(Math.max(0, room.getTimesBooked() - 1));
                // roomRepository.save(room);
            } else {
                log.warn("Room {} not found for release", roomId);
            }
        } catch (Exception e) {
            log.error("Error releasing room {}: {}", roomId, e.getMessage());
        }
    }

    public List<Room> findAvailableRooms() {
        return roomRepository.findByAvailableTrue();
    }

    public List<Room> findRecommendedRooms() {
        return roomRepository.findAvailableRoomsOrderByTimesBooked();
    }

    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
    }

    public List<Room> findRoomsByHotelId(Long hotelId) {
        return roomRepository.findByHotelId(hotelId);
    }

    public Room save(Room room) {
        return roomRepository.save(room);
    }

    public void deleteById(Long id) {
        roomRepository.deleteById(id);
    }
}