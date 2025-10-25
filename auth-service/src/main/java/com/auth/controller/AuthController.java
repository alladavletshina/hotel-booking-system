package com.auth.controller;

import com.auth.dto.AuthRequest;
import com.auth.dto.AuthResponse;
import com.auth.dto.RegisterRequest;
import com.auth.entity.User;
import com.auth.service.AuthService;
import com.auth.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "API для регистрации и аутентификации пользователей")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Operation(
            summary = "Регистрация пользователя",
            description = "Регистрация нового пользователя в системе. По умолчанию создается пользователь с ролью USER. " +
                    "Для создания администратора необходимо указать роль ADMIN в запросе."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка регистрации: пользователь уже существует или неверные данные"
            )
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Проверяем, не существует ли пользователь
            if (authService.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());

            // Роль ADMIN можно установить только если явно указана
            if ("ADMIN".equals(request.getRole())) {
                user.setRole("ADMIN");
            } else {
                user.setRole("USER"); // По умолчанию
            }

            User savedUser = authService.saveUser(user);

            // Генерируем токен для нового пользователя
            String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getRole());

            AuthResponse response = new AuthResponse(
                    token,
                    savedUser.getUsername(),
                    savedUser.getRole(),
                    "Registration successful"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Аутентификация пользователя",
            description = "Вход пользователя в систему. При успешной аутентификации возвращается JWT токен."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Аутентификация прошла успешно",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверные учетные данные"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            var user = authService.authenticate(request.getUsername(), request.getPassword());

            if (user.isPresent()) {
                String token = jwtUtil.generateToken(user.get().getUsername(), user.get().getRole());

                AuthResponse response = new AuthResponse(
                        token,
                        user.get().getUsername(),
                        user.get().getRole(),
                        "Login successful"
                );

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("Invalid credentials");
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Login failed: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Проверка токена",
            description = "Валидация JWT токена. Используется для проверки действительности токена."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Токен валиден"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Токен невалиден или отсутствует"
            )
    })
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        System.out.println("=== SWAGGER DEBUG ===");
        System.out.println("Raw Authorization header: '" + authHeader + "'");

        try {
            // Убираем возможные кавычки и лишние пробелы
            String cleanedHeader = authHeader != null ? authHeader.trim() : "";

            // Убираем кавычки если они есть
            if (cleanedHeader.startsWith("\"") && cleanedHeader.endsWith("\"")) {
                cleanedHeader = cleanedHeader.substring(1, cleanedHeader.length() - 1);
            }

            System.out.println("Cleaned header: '" + cleanedHeader + "'");

            // Проверяем разные варианты Bearer
            if (cleanedHeader.startsWith("Bearer ") || cleanedHeader.startsWith("bearer ")) {
                String token = cleanedHeader.substring(7).trim();
                System.out.println("Extracted token: " + token);

                if (jwtUtil.validateToken(token)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "valid");
                    response.put("message", "Token is valid");
                    response.put("username", jwtUtil.extractUsername(token));
                    response.put("role", jwtUtil.extractRole(token));
                    response.put("received_header", authHeader); // Для отладки
                    response.put("cleaned_header", cleanedHeader); // Для отладки
                    return ResponseEntity.ok(response);
                }
            }

            System.out.println("Invalid header format after cleaning");
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid token",
                    "received_header", authHeader,
                    "cleaned_header", cleanedHeader != null ? cleanedHeader : "null"
            ));

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Token validation failed",
                    "details", e.getMessage()
            ));
        }
    }
}