package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.dto.AuthRequest;
import com.hotelbooking.booking.dto.AuthResponse;
import com.hotelbooking.booking.dto.RegisterRequest;
import com.hotelbooking.booking.dto.UserDto;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.mapper.UserMapper;
import com.hotelbooking.booking.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            log.info("Login attempt for user: {}", authRequest.getUsername());

            if (authRequest.getUsername() == null || authRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body("Username and password are required");
            }

            Optional<User> user = authService.authenticate(authRequest.getUsername(), authRequest.getPassword());

            if (user.isPresent()) {
                AuthResponse response = new AuthResponse(
                        "dummy-token-" + System.currentTimeMillis(),
                        user.get().getUsername(),
                        user.get().getRole(),
                        "Login successful"
                );
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, "Invalid credentials"));
            }
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, "Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            log.info("Registration attempt for user: {}", registerRequest.getUsername());

            if (registerRequest.getUsername() == null || registerRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body("Username and password are required");
            }

            // Создаем User из RegisterRequest
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setPassword(registerRequest.getPassword());
            user.setEmail(registerRequest.getEmail());
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());

            User createdUser = authService.registerUser(user);
            UserDto createdUserDto = userMapper.toDto(createdUser);

            log.info("User registered successfully: {}", createdUser.getUsername());
            return ResponseEntity.ok(createdUserDto);

        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }
}