package com.hotelbooking.booking.integration;

import com.hotelbooking.booking.BookingServiceApplication;
import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BookingServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @WithMockUser(roles = "USER")
    void createBooking_ShouldCreateBooking_WhenValidRequest() throws Exception {
        // Arrange
    }
}