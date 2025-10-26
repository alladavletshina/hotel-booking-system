package com.hotelbooking.booking.service;

import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User createUser(Long id, String username, String email, String firstName, String lastName, String role, Boolean active, String password) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setActive(active);
        user.setPassword(password);
        return user;
    }

    /**
     * Тест для метода: getAllUsers()
     * Назначение: Получение списка всех пользователей
     * Ожидаемый результат:
     * - Возвращает список всех пользователей
     * - Вызывает метод findAll() репозитория
     */
    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Given
        User user1 = createUser(1L, "user1", "user1@example.com", "John", "Doe", "USER", true, "encodedPass1");
        User user2 = createUser(2L, "admin1", "admin1@example.com", "Jane", "Smith", "ADMIN", true, "encodedPass2");
        List<User> expectedUsers = Arrays.asList(user1, user2);

        when(userRepository.findAll()).thenReturn(expectedUsers);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedUsers, result);
        verify(userRepository, times(1)).findAll();
    }

    /**
     * Тест для метода: getUserById()
     * Назначение: Получение пользователя по существующему ID
     * Ожидаемый результат:
     * - Возвращает Optional с пользователем
     * - Вызывает метод findById() репозитория
     */
    @Test
    void getUserById_WithExistingId_ShouldReturnUser() {
        // Given
        Long userId = 1L;
        User expectedUser = createUser(userId, "user1", "user1@example.com", "John", "Doe", "USER", true, "encodedPass");

        when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userService.getUserById(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedUser, result.get());
        verify(userRepository, times(1)).findById(userId);
    }

    /**
     * Тест для метода: getUserById()
     * Назначение: Получение пользователя по несуществующему ID
     * Ожидаемый результат:
     * - Возвращает пустой Optional
     * - Вызывает метод findById() репозитория
     */
    @Test
    void getUserById_WithNonExistingId_ShouldReturnEmptyOptional() {
        // Given
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserById(userId);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findById(userId);
    }

    /**
     * Тест для метода: getUserByUsername()
     * Назначение: Получение пользователя по существующему username
     * Ожидаемый результат:
     * - Возвращает Optional с пользователем
     * - Вызывает метод findByUsername() репозитория
     */
    @Test
    void getUserByUsername_WithExistingUsername_ShouldReturnUser() {
        // Given
        String username = "user1";
        User expectedUser = createUser(1L, username, "user1@example.com", "John", "Doe", "USER", true, "encodedPass");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userService.getUserByUsername(username);

        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedUser, result.get());
        verify(userRepository, times(1)).findByUsername(username);
    }

    /**
     * Тест для метода: createUser()
     * Назначение: Создание нового пользователя с уникальными данными
     * Ожидаемый результат:
     * - Возвращает созданного пользователя
     * - Кодирует пароль
     * - Устанавливает активный статус
     * - Устанавливает роль USER по умолчанию
     * - Сохраняет пользователя
     */
    @Test
    void createUser_WithUniqueData_ShouldCreateUser() {
        // Given
        User userToCreate = createUser(null, "newuser", "newuser@example.com", "New", "User", null, null, "plainPassword");
        User expectedUser = createUser(1L, "newuser", "newuser@example.com", "New", "User", "USER", true, "encodedPassword");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(userToCreate)).thenReturn(expectedUser);

        // When
        User result = userService.createUser(userToCreate);

        // Then
        assertNotNull(result);
        assertEquals(expectedUser, result);
        assertEquals("encodedPassword", userToCreate.getPassword());
        assertTrue(userToCreate.getActive());
        assertEquals("USER", userToCreate.getRole());
        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(passwordEncoder, times(1)).encode("plainPassword");
        verify(userRepository, times(1)).save(userToCreate);
    }

    /**
     * Тест для метода: createUser()
     * Назначение: Создание нового пользователя с существующим username
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением "Username already exists"
     * - Не сохраняет пользователя
     */
    @Test
    void createUser_WithExistingUsername_ShouldThrowException() {
        // Given
        User userToCreate = createUser(null, "existinguser", "new@example.com", "New", "User", "USER", true, "password");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.createUser(userToCreate));

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, times(1)).existsByUsername("existinguser");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Тест для метода: createUser()
     * Назначение: Создание нового пользователя с существующим email
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением "Email already exists"
     * - Не сохраняет пользователя
     */
    @Test
    void createUser_WithExistingEmail_ShouldThrowException() {
        // Given
        User userToCreate = createUser(null, "newuser", "existing@example.com", "New", "User", "USER", true, "password");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.createUser(userToCreate));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("existing@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Тест для метода: createUser()
     * Назначение: Создание пользователя с указанной ролью
     * Ожидаемый результат:
     * - Сохраняет указанную роль
     * - Не устанавливает роль по умолчанию
     */
    @Test
    void createUser_WithSpecifiedRole_ShouldUseSpecifiedRole() {
        // Given
        User userToCreate = createUser(null, "adminuser", "admin@example.com", "Admin", "User", "ADMIN", null, "password");
        User expectedUser = createUser(1L, "adminuser", "admin@example.com", "Admin", "User", "ADMIN", true, "encodedPassword");

        when(userRepository.existsByUsername("adminuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(userToCreate)).thenReturn(expectedUser);

        // When
        User result = userService.createUser(userToCreate);

        // Then
        assertNotNull(result);
        assertEquals("ADMIN", userToCreate.getRole());
        verify(userRepository, times(1)).save(userToCreate);
    }

    /**
     * Тест для метода: updateUser()
     * Назначение: Обновление существующего пользователя
     * Ожидаемый результат:
     * - Возвращает обновленного пользователя
     * - Обновляет разрешенные поля
     * - Сохраняет пользователя
     */
    @Test
    void updateUser_WithExistingUser_ShouldUpdateUser() {
        // Given
        Long userId = 1L;
        User existingUser = createUser(userId, "olduser", "old@example.com", "Old", "User", "USER", true, "password");
        User userDetails = createUser(null, "newuser", "new@example.com", "New", "Name", "ADMIN", false, "newpassword");
        User expectedUser = createUser(userId, "olduser", "new@example.com", "New", "Name", "ADMIN", false, "password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(existingUser)).thenReturn(expectedUser);

        // When
        User result = userService.updateUser(userId, userDetails);

        // Then
        assertNotNull(result);
        assertEquals("New", existingUser.getFirstName());
        assertEquals("Name", existingUser.getLastName());
        assertEquals("new@example.com", existingUser.getEmail());
        assertEquals("ADMIN", existingUser.getRole());
        assertFalse(existingUser.getActive());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).existsByEmail("new@example.com");
        verify(userRepository, times(1)).save(existingUser);
    }

    /**
     * Тест для метода: updateUser()
     * Назначение: Обновление несуществующего пользователя
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением "User not found"
     * - Не сохраняет пользователя
     */
    @Test
    void updateUser_WithNonExistingUser_ShouldThrowException() {
        // Given
        Long userId = 999L;
        User userDetails = createUser(null, "newuser", "new@example.com", "New", "Name", "ADMIN", false, "password");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(userId, userDetails));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Тест для метода: updateUser()
     * Назначение: Обновление пользователя с существующим email
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением "Email already exists"
     * - Не сохраняет пользователя
     */
    @Test
    void updateUser_WithExistingEmail_ShouldThrowException() {
        // Given
        Long userId = 1L;
        User existingUser = createUser(userId, "user1", "old@example.com", "Old", "User", "USER", true, "password");
        User userDetails = createUser(null, "user1", "existing@example.com", "New", "Name", "USER", true, "password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(userId, userDetails));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Тест для метода: updateUser()
     * Назначение: Обновление пользователя с тем же email
     * Ожидаемый результат:
     * - Не проверяет существование email
     * - Сохраняет пользователя
     */
    @Test
    void updateUser_WithSameEmail_ShouldNotCheckEmailExistence() {
        // Given
        Long userId = 1L;
        User existingUser = createUser(userId, "user1", "same@example.com", "Old", "User", "USER", true, "password");
        User userDetails = createUser(null, "user1", "same@example.com", "New", "Name", "ADMIN", false, "password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        // When
        User result = userService.updateUser(userId, userDetails);

        // Then
        assertNotNull(result);
        assertEquals("same@example.com", existingUser.getEmail());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, times(1)).save(existingUser);
    }

    /**
     * Тест для метода: updateUser()
     * Назначение: Частичное обновление пользователя
     * Ожидаемый результат:
     * - Обновляет только указанные поля
     * - Сохраняет пользователя
     */
    @Test
    void updateUser_WithPartialData_ShouldUpdateOnlySpecifiedFields() {
        // Given
        Long userId = 1L;
        User existingUser = createUser(userId, "user1", "user@example.com", "Old", "User", "USER", true, "password");
        User userDetails = createUser(null, null, null, "New", null, null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        // When
        User result = userService.updateUser(userId, userDetails);

        // Then
        assertNotNull(result);
        assertEquals("New", existingUser.getFirstName());
        assertEquals("User", existingUser.getLastName()); // осталось прежним
        assertEquals("user@example.com", existingUser.getEmail()); // осталось прежним
        assertEquals("USER", existingUser.getRole()); // осталось прежним
        assertTrue(existingUser.getActive()); // осталось прежним
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, times(1)).save(existingUser);
    }

    /**
     * Тест для метода: deactivateUser()
     * Назначение: Деактивация существующего пользователя
     * Ожидаемый результат:
     * - Устанавливает active = false
     * - Сохраняет пользователя
     */
    @Test
    void deactivateUser_WithExistingUser_ShouldDeactivateUser() {
        // Given
        Long userId = 1L;
        User user = createUser(userId, "user1", "user@example.com", "John", "Doe", "USER", true, "password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.deactivateUser(userId);

        // Then
        assertFalse(user.getActive());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(user);
    }

    /**
     * Тест для метода: deactivateUser()
     * Назначение: Деактивация несуществующего пользователя
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением "User not found"
     * - Не сохраняет пользователя
     */
    @Test
    void deactivateUser_WithNonExistingUser_ShouldThrowException() {
        // Given
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deactivateUser(userId));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Тест для метода: activateUser()
     * Назначение: Активация существующего пользователя
     * Ожидаемый результат:
     * - Устанавливает active = true
     * - Сохраняет пользователя
     */
    @Test
    void activateUser_WithExistingUser_ShouldActivateUser() {
        // Given
        Long userId = 1L;
        User user = createUser(userId, "user1", "user@example.com", "John", "Doe", "USER", false, "password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.activateUser(userId);

        // Then
        assertTrue(user.getActive());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(user);
    }

    /**
     * Тест для метода: activateUser()
     * Назначение: Активация несуществующего пользователя
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением "User not found"
     * - Не сохраняет пользователя
     */
    @Test
    void activateUser_WithNonExistingUser_ShouldThrowException() {
        // Given
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.activateUser(userId));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Тест для метода: deleteUser()
     * Назначение: Удаление существующего пользователя
     * Ожидаемый результат:
     * - Вызывает метод deleteById() репозитория
     * - Не выбрасывает исключений
     */
    @Test
    void deleteUser_WithExistingUser_ShouldDeleteUser() {
        // Given
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository, times(1)).existsById(userId);
        verify(userRepository, times(1)).deleteById(userId);
    }

    /**
     * Тест для метода: deleteUser()
     * Назначение: Удаление несуществующего пользователя
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением "User not found"
     * - Не вызывает метод deleteById()
     */
    @Test
    void deleteUser_WithNonExistingUser_ShouldThrowException() {
        // Given
        Long userId = 999L;

        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deleteUser(userId));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).existsById(userId);
        verify(userRepository, never()).deleteById(anyLong());
    }
}