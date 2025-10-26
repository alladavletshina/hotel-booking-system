package com.auth.service;

import com.auth.entity.User;
import com.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Подготовка тестового пользователя перед каждым тестом
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
    }

    /**
     * Тест: Поиск пользователя по имени пользователя
     * Действие: Поиск существующего пользователя
     * Ожидание: Должен вернуть пользователя, когда пользователь существует
     */
    @Test
    void findByUsername_ShouldReturnUser_WhenUserExists() {
        // Arrange - настройка моков
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act - вызов тестируемого метода
        Optional<User> result = authService.findByUsername("testuser");

        // Assert - проверка результатов
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    /**
     * Тест: Сохранение пользователя
     * Действие: Сохранение пользователя в репозитории
     * Ожидание: Должен вернуть сохраненного пользователя
     */
    @Test
    void saveUser_ShouldReturnSavedUser() {
        // Arrange - настройка моков
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act - вызов тестируемого метода
        User result = authService.saveUser(testUser);

        // Assert - проверка результатов и вызовов
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).save(testUser);
    }

    /**
     * Тест: Аутентификация пользователя
     * Действие: Попытка аутентификации с правильными учетными данными
     * Ожидание: Должен вернуть пользователя при валидных учетных данных
     */
    @Test
    void authenticate_ShouldReturnUser_WhenValidCredentials() {
        // Arrange - настройка моков
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        // Act - вызов тестируемого метода
        Optional<User> result = authService.authenticate("testuser", "password");

        // Assert - проверка результатов
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    /**
     * Тест: Аутентификация пользователя с неверным паролем
     * Действие: Попытка аутентификации с неправильным паролем
     * Ожидание: Должен вернуть пустой Optional при неверном пароле
     */
    @Test
    void authenticate_ShouldReturnEmpty_WhenInvalidPassword() {
        // Arrange - настройка моков
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        // Act - вызов тестируемого метода
        Optional<User> result = authService.authenticate("testuser", "wrongpassword");

        // Assert - проверка результатов
        assertFalse(result.isPresent());
    }
}