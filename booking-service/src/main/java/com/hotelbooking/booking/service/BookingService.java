package com.hotelbooking.booking.service;

import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @Transactional
    public Booking createBooking(Booking booking, String correlationId) {
        // Проверяем идемпотентность
        if (correlationId != null && bookingRepository.existsByCorrelationId(correlationId)) {
            return bookingRepository.findByCorrelationId(correlationId)
                    .orElseThrow(() -> new RuntimeException("Duplicate booking request"));
        }

        // Проверяем даты
        validateBookingDates(booking);

        // Временно пропускаем проверку доступности номера
        // TODO: Добавить интеграцию с Hotel Service позже
        log.warn("Room availability check skipped - Hotel Service integration not implemented");

        // Сохраняем бронирование
        booking.setCorrelationId(correlationId != null ? correlationId : UUID.randomUUID().toString());
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created successfully: ID {}, Room {}, User {}",
                savedBooking.getId(), savedBooking.getRoomId(), savedBooking.getUserId());

        return savedBooking;
    }

    public List<Booking> getUserBookings(Long userId) {
        // Проверяем, что пользователь запрашивает свои бронирования
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            Long currentUserId = extractUserIdFromJwt(jwt);

            // Если userId не совпадает с текущим пользователем, проверяем права
            if (currentUserId != null && !currentUserId.equals(userId)) {
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                if (!isAdmin) {
                    throw new RuntimeException("Access denied");
                }
            }
        }

        return bookingRepository.findByUserId(userId);
    }

    public List<Booking> getCurrentUserBookings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new RuntimeException("User not authenticated");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        Long userId = extractUserIdFromJwt(jwt);
        if (userId == null) {
            String username = jwt.getClaimAsString("sub");
            userId = generateUserIdFromUsername(username);
        }

        return bookingRepository.findByUserId(userId);
    }

    private Long extractUserIdFromJwt(Jwt jwt) {
        try {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim instanceof Long) {
                return (Long) userIdClaim;
            } else if (userIdClaim instanceof Integer) {
                return ((Integer) userIdClaim).longValue();
            } else if (userIdClaim instanceof String) {
                return Long.parseLong((String) userIdClaim);
            }
        } catch (Exception e) {
            log.debug("UserId not found in JWT token or cannot be converted to Long");
        }
        return null;
    }

    private Long generateUserIdFromUsername(String username) {
        return (long) Math.abs(username.hashCode());
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

        if (booking.getStartDate().plusDays(30).isBefore(booking.getEndDate())) {
            throw new RuntimeException("Booking cannot exceed 30 days");
        }
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        // Проверяем права доступа
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            Long currentUserId = extractUserIdFromJwt(jwt);

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin && (currentUserId == null || !currentUserId.equals(booking.getUserId()))) {
                throw new RuntimeException("Access denied");
            }
        }

        if (booking.getStatus() == BookingStatus.CANCELLED ||
                booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel booking with status: " + booking.getStatus());
        }

        // Временно пропускаем освобождение номера
        // TODO: Добавить интеграцию с Hotel Service позже
        log.warn("Room release skipped - Hotel Service integration not implemented");

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());

        Booking cancelledBooking = bookingRepository.save(booking);
        log.info("Booking {} cancelled successfully", bookingId);

        return cancelledBooking;
    }

    // Остальные методы...
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
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
        // Временно всегда возвращаем true
        // TODO: Добавить интеграцию с Hotel Service позже
        log.warn("Room availability check always returning true - Hotel Service integration not implemented");
        return true;
    }
}