package com.hotelbooking.booking.service;

import com.hotelbooking.booking.client.HotelServiceClient;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelServiceClient hotelServiceClient;

    @Mock
    private InternalAuthService internalAuthService;

    @InjectMocks
    private BookingService bookingService;

    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUserId(1L);
        testBooking.setUsername("testuser");
        testBooking.setRoomId(101L);
        testBooking.setStartDate(LocalDate.now().plusDays(1));
        testBooking.setEndDate(LocalDate.now().plusDays(3));
        testBooking.setStatus(BookingStatus.PENDING);
    }

    @Test
    void createBooking_ShouldConfirmBooking_WhenRoomAvailable() {
        // Arrange
        when(bookingRepository.existsByCorrelationId(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(internalAuthService.isTokenValid()).thenReturn(true);
        when(hotelServiceClient.confirmAvailability(anyLong())).thenReturn(true);

        // Act
        Booking result = bookingService.createBooking(testBooking, "correlation-123");

        // Assert
        assertNotNull(result);
        verify(bookingRepository, times(2)).save(any(Booking.class));
    }

    @Test
    void cancelBooking_ShouldCancelBooking_WhenBookingExists() {
        // Arrange
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        // Act
        Booking result = bookingService.cancelBooking(1L);

        // Assert
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(hotelServiceClient, times(1)).releaseRoom(anyLong());
    }

    @Test
    void getBookingById_ShouldReturnBooking_WhenExists() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // Act
        Optional<Booking> result = bookingService.getBookingById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }
}