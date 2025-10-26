package com.auth.controller;

import com.auth.dto.AuthRequest;
import com.auth.dto.RegisterRequest;
import com.auth.entity.User;
import com.auth.service.AuthService;
import com.auth.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    /**
     * Тест для endpoint: POST /auth/register
     * Назначение: Регистрация нового пользователя в системе
     * Сценарий: Успешная регистрация с валидными данными
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает JWT токен в ответе
     * - Возвращает username и роль пользователя
     * Бизнес-логика:
     * 1. Проверяет, что пользователь с таким username не существует
     * 2. Кодирует пароль
     * 3. Сохраняет пользователя в базу данных
     * 4. Генерирует JWT токен для нового пользователя
     */
    @Test
    void register_ShouldReturnToken_WhenValidRequest() throws Exception {
        // Arrange - подготовка тестовых данных и моков
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setRole("USER");

        when(authService.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(authService.saveUser(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("jwt-token");

        // Act & Assert - выполнение запроса и проверка результатов
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    /**
     * Тест для endpoint: POST /auth/login
     * Назначение: Аутентификация существующего пользователя
     * Сценарий: Успешный вход с правильными учетными данными
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает JWT токен для доступа к защищенным ресурсам
     * - Возвращает информацию о пользователе
     * Бизнес-логика:
     * 1. Проверяет учетные данные (username/password)
     * 2. При успешной аутентификации генерирует JWT токен
     * 3. Возвращает токен и информацию о пользователе
     */
    @Test
    void login_ShouldReturnToken_WhenValidCredentials() throws Exception {
        // Arrange - подготовка тестовых данных и моков
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        User user = new User();
        user.setUsername("testuser");
        user.setRole("USER");

        when(authService.authenticate(anyString(), anyString())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("jwt-token");

        // Act & Assert - выполнение запроса и проверка результатов
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    /**
     * Тест для endpoint: POST /auth/login
     * Назначение: Аутентификация существующего пользователя
     * Сценарий: Неудачная попытка входа с неверными учетными данными
     * Ожидаемый результат:
     * - HTTP статус 400 (Bad Request)
     * - Возвращает текстовое сообщение об ошибке
     * Бизнес-логика:
     * 1. Проверяет учетные данные (username/password)
     * 2. При неверных данных возвращает ошибку аутентификации
     * 3. Не генерирует JWT токен
     */
    @Test
    void login_ShouldReturnBadRequest_WhenInvalidCredentials() throws Exception {
        // Arrange - подготовка тестовых данных и моков
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(authService.authenticate(anyString(), anyString())).thenReturn(Optional.empty());

        // Act & Assert - выполнение запроса и проверка результатов
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid credentials"));
    }
}