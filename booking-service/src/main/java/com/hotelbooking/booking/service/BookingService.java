package com.hotelbooking.booking.service;

import com.hotelbooking.booking.client.HotelServiceClient;
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
    private final HotelServiceClient hotelServiceClient;
    private final InternalAuthService internalAuthService;

    @Transactional
    public Booking createBooking(Booking booking, String correlationId) {
        log.info("Creating booking with correlationId: {}", correlationId);

        // Проверяем идемпотентность
        if (correlationId != null && bookingRepository.existsByCorrelationId(correlationId)) {
            log.info("Duplicate booking request with correlationId: {}", correlationId);
            return bookingRepository.findByCorrelationId(correlationId)
                    .orElseThrow(() -> new RuntimeException("Duplicate booking request"));
        }

        // Проверяем даты
        validateBookingDates(booking);

        // Устанавливаем начальные значения
        booking.setCorrelationId(correlationId != null ? correlationId : UUID.randomUUID().toString());
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());

        // Сохраняем бронирование в статусе PENDING
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created with PENDING status: ID {}, Room {}, User {}",
                savedBooking.getId(), savedBooking.getRoomId(), savedBooking.getUserId());

        try {
            // Проверяем, что internal аутентификация настроена
            if (!internalAuthService.isTokenValid()) {
                log.error("Internal authentication not available");
                handleBookingFailure(savedBooking, "Internal service authentication failed");
                throw new RuntimeException("Service temporarily unavailable");
            }

            // Шаг 1: Подтверждаем доступность номера через Hotel Service
            log.info("Confirming availability for room {} via Hotel Service", savedBooking.getRoomId());
            Boolean isAvailable = hotelServiceClient.confirmAvailability(savedBooking.getRoomId());

            if (Boolean.TRUE.equals(isAvailable)) {
                // Шаг 2: Если доступно - подтверждаем бронирование
                savedBooking.setStatus(BookingStatus.CONFIRMED);
                savedBooking.setUpdatedAt(LocalDateTime.now());

                Booking confirmedBooking = bookingRepository.save(savedBooking);
                log.info("Booking CONFIRMED: ID {}", confirmedBooking.getId());
                return confirmedBooking;
            } else {
                // Шаг 3: Если недоступно - отменяем бронирование
                log.warn("Room {} not available, cancelling booking {}", savedBooking.getRoomId(), savedBooking.getId());
                handleBookingFailure(savedBooking, "Room not available");
                throw new RuntimeException("Room is not available for selected dates");
            }

        } catch (Exception e) {
            // Шаг 4: При любой ошибке - компенсирующее действие
            log.error("Error during booking confirmation for booking {}: {}", savedBooking.getId(), e.getMessage());
            handleBookingFailure(savedBooking, "Error during booking confirmation: " + e.getMessage());
            throw new RuntimeException("Booking failed: " + e.getMessage());
        }
    }

    private void handleBookingFailure(Booking booking, String reason) {
        try {
            // Освобождаем номер в Hotel Service (компенсирующее действие)
            if (booking.getRoomId() != null) {
                log.info("Releasing room {} due to booking failure", booking.getRoomId());
                try {
                    hotelServiceClient.releaseRoom(booking.getRoomId());
                } catch (Exception e) {
                    log.error("Error releasing room {}: {}", booking.getRoomId(), e.getMessage());
                }
            }

            // Обновляем статус бронирования
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            log.warn("Booking CANCELLED: ID {}. Reason: {}", booking.getId(), reason);

        } catch (Exception e) {
            log.error("Error during booking failure handling for booking {}: {}",
                    booking.getId(), e.getMessage());
        }
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

        // Освобождаем номер в Hotel Service
        if (booking.getRoomId() != null) {
            log.info("Releasing room {} due to booking cancellation", booking.getRoomId());
            try {
                hotelServiceClient.releaseRoom(booking.getRoomId());
            } catch (Exception e) {
                log.error("Error releasing room {}: {}", booking.getRoomId(), e.getMessage());
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());

        Booking cancelledBooking = bookingRepository.save(booking);
        log.info("Booking {} cancelled successfully", bookingId);

        return cancelledBooking;
    }

    public List<Booking> getUserBookings(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            Long currentUserId = extractUserIdFromJwt(jwt);

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
            // Сначала пробуем получить userId из claims
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null) {
                if (userIdClaim instanceof Long) {
                    return (Long) userIdClaim;
                } else if (userIdClaim instanceof Integer) {
                    return ((Integer) userIdClaim).longValue();
                } else if (userIdClaim instanceof String) {
                    return Long.parseLong((String) userIdClaim);
                }
            }

            // Если userId не найден, используем username для генерации стабильного ID
            String username = jwt.getSubject();
            if (username != null) {
                return generateUserIdFromUsername(username);
            }

            log.warn("Neither userId nor username found in JWT token");
            return null;

        } catch (Exception e) {
            log.debug("Error extracting user ID from JWT: {}", e.getMessage());
            return null;
        }
    }


    private Long generateUserIdFromUsername(String username) {
        // Генерируем стабильный числовой ID из username
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
}