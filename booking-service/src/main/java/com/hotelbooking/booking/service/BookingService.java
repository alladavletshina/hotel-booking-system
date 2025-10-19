package com.hotelbooking.booking.service;

import com.hotelbooking.booking.client.HotelServiceClient;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final HotelServiceClient hotelServiceClient;

    @Transactional
    public Booking createBooking(Booking booking, String correlationId) {
        // Проверяем идемпотентность
        if (correlationId != null && bookingRepository.existsByCorrelationId(correlationId)) {
            return bookingRepository.findByCorrelationId(correlationId)
                    .orElseThrow(() -> new RuntimeException("Duplicate booking request"));
        }

        // Проверяем существование пользователя
        User user = userService.getUserById(booking.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Проверяем даты
        validateBookingDates(booking);

        // Проверяем доступность номера через Hotel Service
        Boolean isAvailable;
        try {
            isAvailable = hotelServiceClient.confirmAvailability(booking.getRoomId());
            log.info("Room {} availability confirmed: {}", booking.getRoomId(), isAvailable);
        } catch (Exception e) {
            log.error("Error calling Hotel Service for availability check: {}", e.getMessage());
            throw new RuntimeException("Cannot check room availability at the moment. Please try again.");
        }

        if (!Boolean.TRUE.equals(isAvailable)) {
            throw new RuntimeException("Room " + booking.getRoomId() + " is not available for the selected dates");
        }

        // Сохраняем бронирование
        booking.setUser(user);
        booking.setCorrelationId(correlationId != null ? correlationId : UUID.randomUUID().toString());
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created successfully: ID {}, Room {}, User {}",
                savedBooking.getId(), savedBooking.getRoomId(), savedBooking.getUser().getId());

        return savedBooking;
    }

    private void validateBookingDates(Booking booking) {
        LocalDate today = LocalDate.now();

        if (booking.getStartDate() == null || booking.getEndDate() == null) {
            throw new RuntimeException("Start date and end date are required");
        }

        if (booking.getStartDate().isBefore(today)) {
            throw new RuntimeException("Start date cannot be in the past");
        }

        if (booking.getEndDate().isBefore(booking.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }

        if (booking.getStartDate().equals(booking.getEndDate())) {
            throw new RuntimeException("Start and end dates cannot be the same");
        }

        // Проверяем, что бронирование не более чем на 30 дней
        if (booking.getStartDate().plusDays(30).isBefore(booking.getEndDate())) {
            throw new RuntimeException("Booking cannot exceed 30 days");
        }
    }

    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED ||
                booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel booking with status: " + booking.getStatus());
        }

        // Освобождаем номер в Hotel Service
        try {
            hotelServiceClient.releaseRoom(booking.getRoomId());
            log.info("Room {} released for cancelled booking {}", booking.getRoomId(), bookingId);
        } catch (Exception e) {
            log.error("Failed to release room {}: {}", booking.getRoomId(), e.getMessage());
            // Все равно отменяем бронирование, но логируем ошибку
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());

        Booking cancelledBooking = bookingRepository.save(booking);
        log.info("Booking {} cancelled successfully", bookingId);

        return cancelledBooking;
    }

    @Transactional
    public Booking updateBookingStatus(Long bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        booking.setStatus(status);
        booking.setUpdatedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    @Transactional
    public void completeExpiredBookings() {
        List<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED);
        LocalDate today = LocalDate.now();
        int completedCount = 0;

        for (Booking booking : confirmedBookings) {
            if (booking.getEndDate().isBefore(today)) {
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);
                completedCount++;
                log.debug("Booking {} marked as completed", booking.getId());
            }
        }

        if (completedCount > 0) {
            log.info("Completed {} expired bookings", completedCount);
        }
    }

    public boolean isRoomAvailableForDates(Long roomId, LocalDate startDate, LocalDate endDate) {
        // Здесь можно добавить дополнительную логику проверки дат
        // Пока полагаемся на проверку через Hotel Service
        try {
            return hotelServiceClient.confirmAvailability(roomId);
        } catch (Exception e) {
            log.error("Error checking room availability: {}", e.getMessage());
            return false;
        }
    }
}