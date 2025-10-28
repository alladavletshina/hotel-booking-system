package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.entity.BookingSlot;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.repository.BookingSlotRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingSlotRepository bookingSlotRepository;

    /**
     * Проверка доступности номера на конкретные даты
     */
    public boolean isRoomAvailable(Long roomId, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty() || !roomOpt.get().getAvailable()) {
            log.debug("Room {} is not available", roomId);
            return false;
        }

        boolean hasConflict = bookingSlotRepository.hasDateConflict(roomId, startDate, endDate);

        log.debug("Room {} availability check: {} (has conflict: {})",
                roomId, !hasConflict, hasConflict);
        return !hasConflict;
    }

    /**
     * Подтверждение доступности с временной блокировкой
     */
    @Transactional
    public boolean confirmAvailability(Long roomId, LocalDate startDate, LocalDate endDate, Long bookingId) {
        try {
            validateDates(startDate, endDate);
            log.info("Confirming availability for room {} from {} to {} (booking: {})",
                    roomId, startDate, endDate, bookingId);

            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isEmpty() || !roomOpt.get().getAvailable()) {
                log.warn("Room {} is not available", roomId);
                return false;
            }

            if (bookingSlotRepository.hasDateConflict(roomId, startDate, endDate)) {
                List<BookingSlot> conflicts = bookingSlotRepository.findConflictingSlots(roomId, startDate, endDate);
                log.warn("Room {} has date conflict for period {} - {}. Conflicts: {}",
                        roomId, startDate, endDate, conflicts.size());
                return false;
            }

            BookingSlot tempSlot = new BookingSlot();
            tempSlot.setRoomId(roomId);
            tempSlot.setStartDate(startDate);
            tempSlot.setEndDate(endDate);
            tempSlot.setBookingId(bookingId);
            tempSlot.setStatus("RESERVED");

            bookingSlotRepository.save(tempSlot);

            Room room = roomOpt.get();
            room.setTimesBooked(room.getTimesBooked() != null ? room.getTimesBooked() + 1 : 1);
            roomRepository.save(room);

            log.info("Room {} availability confirmed and temporarily reserved. Times booked: {}",
                    roomId, room.getTimesBooked());
            return true;

        } catch (Exception e) {
            log.error("Error confirming availability for room {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    /**
     * Освобождение номера (компенсирующее действие)
     */
    @Transactional
    public void releaseRoom(Long roomId, Long bookingId) {
        try {
            log.info("Releasing room {} for booking {}", roomId, bookingId);

            List<BookingSlot> slots = bookingSlotRepository.findByBookingId(bookingId);
            int releasedCount = 0;

            for (BookingSlot slot : slots) {
                if ("RESERVED".equals(slot.getStatus())) {
                    bookingSlotRepository.delete(slot);
                    releasedCount++;
                    log.debug("Removed temporary slot {} for room {}", slot.getId(), roomId);
                }
            }

            log.info("Room {} released from temporary reservation for booking {}. Released {} slots",
                    roomId, bookingId, releasedCount);

        } catch (Exception e) {
            log.error("Error releasing room {}: {}", roomId, e.getMessage());
        }
    }

    /**
     * Подтверждение бронирования (перевод из RESERVED в CONFIRMED)
     */
    @Transactional
    public void confirmBooking(Long roomId, Long bookingId) {
        try {
            List<BookingSlot> slots = bookingSlotRepository.findByBookingId(bookingId);
            int confirmedCount = 0;

            for (BookingSlot slot : slots) {
                if ("RESERVED".equals(slot.getStatus())) {
                    slot.setStatus("CONFIRMED");
                    bookingSlotRepository.save(slot);
                    confirmedCount++;
                    log.info("Booking slot {} confirmed for room {}", slot.getId(), roomId);
                }
            }

            log.info("Confirmed {} booking slots for room {} (booking: {})",
                    confirmedCount, roomId, bookingId);

        } catch (Exception e) {
            log.error("Error confirming booking for room {}: {}", roomId, e.getMessage());
            throw new RuntimeException("Failed to confirm booking: " + e.getMessage());
        }
    }

    /**
     * Отмена бронирования
     */
    @Transactional
    public void cancelBooking(Long roomId, Long bookingId) {
        try {
            List<BookingSlot> slots = bookingSlotRepository.findByBookingId(bookingId);
            int cancelledCount = 0;

            for (BookingSlot slot : slots) {
                slot.setStatus("CANCELLED");
                bookingSlotRepository.save(slot);
                cancelledCount++;
                log.debug("Cancelled booking slot {} for room {}", slot.getId(), roomId);
            }

            log.info("Cancelled {} booking slots for room {} (booking: {})",
                    cancelledCount, roomId, bookingId);

        } catch (Exception e) {
            log.error("Error cancelling booking for room {}: {}", roomId, e.getMessage());
        }
    }

    /**
     * Поиск доступных номеров на указанные даты
     */
    public List<Room> findAvailableRooms(LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);

        List<Room> allAvailableRooms = roomRepository.findByAvailableTrue();

        return allAvailableRooms.stream()
                .filter(room -> !bookingSlotRepository.hasDateConflict(room.getId(), startDate, endDate))
                .toList();
    }

    /**
     * Получить рекомендованные номера на указанные даты
     */
    public List<Room> findRecommendedRooms(LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);

        List<Room> availableRooms = findAvailableRooms(startDate, endDate);

        return availableRooms.stream()
                .sorted((r1, r2) -> {
                    int booked1 = r1.getTimesBooked() != null ? r1.getTimesBooked() : 0;
                    int booked2 = r2.getTimesBooked() != null ? r2.getTimesBooked() : 0;
                    return Integer.compare(booked1, booked2);
                })
                .toList();
    }

    /**
     * Очистка устаревших временных бронирований (выполняется по расписанию)
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredReservations() {
        try {
            LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(10); // 10 минут назад
            List<BookingSlot> expiredSlots = bookingSlotRepository.findExpiredReservations(null, expiryTime);

            if (!expiredSlots.isEmpty()) {
                bookingSlotRepository.deleteAll(expiredSlots);
                log.info("Cleaned up {} expired temporary reservations", expiredSlots.size());
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired reservations: {}", e.getMessage());
        }
    }

    /**
     * Валидация дат
     */
    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (startDate.plusMonths(1).isBefore(endDate)) {
            throw new IllegalArgumentException("Booking period cannot exceed 1 month");
        }
    }

    /**
     * Автоматический подбор лучшей комнаты на указанные даты
     */
    public Room findBestAvailableRoom(LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);

        log.info("Finding best available room for dates {} to {}", startDate, endDate);

        List<Room> recommendedRooms = findRecommendedRooms(startDate, endDate);

        if (recommendedRooms.isEmpty()) {
            log.warn("No available rooms found for dates {} to {}", startDate, endDate);
            throw new RuntimeException("No available rooms found for selected dates");
        }

        Room bestRoom = recommendedRooms.get(0);
        log.info("Auto-selected room {} (type: {}, price: {})",
                bestRoom.getId(), bestRoom.getType(), bestRoom.getPrice());

        return bestRoom;
    }

    /**
     * Найти несколько лучших вариантов для выбора
     */
    public List<Room> findTopAvailableRooms(LocalDate startDate, LocalDate endDate, int limit) {
        validateDates(startDate, endDate);

        List<Room> recommendedRooms = findRecommendedRooms(startDate, endDate);

        if (recommendedRooms.size() > limit) {
            return recommendedRooms.subList(0, limit);
        }

        return recommendedRooms;
    }

    @Transactional
    public boolean confirmAvailability(Long roomId) {
        log.warn("Using deprecated confirmAvailability method without dates");
        return confirmAvailability(roomId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), null);
    }

    @Transactional
    public void releaseRoom(Long roomId) {
        log.warn("Using deprecated releaseRoom method without bookingId");
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