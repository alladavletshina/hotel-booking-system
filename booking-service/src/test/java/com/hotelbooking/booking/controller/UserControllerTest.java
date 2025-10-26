package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.dto.UserDto;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.mapper.UserMapper;
import com.hotelbooking.booking.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    private User createUser(Long id, String username, String email, String firstName, String lastName, String role, Boolean active) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setActive(active);
        return user;
    }

    private UserDto createUserDto(Long id, String username, String password, String email, String firstName, String lastName, String role, Boolean active) {
        UserDto userDto = new UserDto();
        userDto.setId(id);
        userDto.setUsername(username);
        userDto.setPassword(password);
        userDto.setEmail(email);
        userDto.setFirstName(firstName);
        userDto.setLastName(lastName);
        userDto.setRole(role);
        userDto.setActive(active);
        return userDto;
    }

    /**
     * Тест для endpoint: GET /admin/users
     * Назначение: Получение списка всех пользователей
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает список пользователей в формате DTO
     * - Вызывает сервис для получения всех пользователей
     */
    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        // Given
        User user1 = createUser(1L, "user1", "user1@example.com", "John", "Doe", "USER", true);
        User user2 = createUser(2L, "admin1", "admin1@example.com", "Jane", "Smith", "ADMIN", true);
        List<User> users = Arrays.asList(user1, user2);

        UserDto userDto1 = createUserDto(1L, "user1", null, "user1@example.com", "John", "Doe", "USER", true);
        UserDto userDto2 = createUserDto(2L, "admin1", null, "admin1@example.com", "Jane", "Smith", "ADMIN", true);
        List<UserDto> expectedDtos = Arrays.asList(userDto1, userDto2);

        when(userService.getAllUsers()).thenReturn(users);
        when(userMapper.toDto(user1)).thenReturn(userDto1);
        when(userMapper.toDto(user2)).thenReturn(userDto2);

        // When
        ResponseEntity<List<UserDto>> response = userController.getAllUsers();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(expectedDtos, response.getBody());
        verify(userService, times(1)).getAllUsers();
        verify(userMapper, times(2)).toDto(any(User.class));
    }

    /**
     * Тест для endpoint: GET /admin/users/{id}
     * Назначение: Получение пользователя по существующему ID
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает пользователя в формате DTO
     * - Вызывает сервис для поиска пользователя по ID
     */
    @Test
    void getUserById_WithExistingId_ShouldReturnUser() {
        // Given
        Long userId = 1L;
        User user = createUser(userId, "user1", "user1@example.com", "John", "Doe", "USER", true);
        UserDto expectedDto = createUserDto(userId, "user1", null, "user1@example.com", "John", "Doe", "USER", true);

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        // When
        ResponseEntity<UserDto> response = userController.getUserById(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedDto, response.getBody());
        verify(userService, times(1)).getUserById(userId);
        verify(userMapper, times(1)).toDto(user);
    }

    /**
     * Тест для endpoint: GET /admin/users/{id}
     * Назначение: Попытка получения пользователя по несуществующему ID
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением "User not found"
     * - Вызывает сервис для поиска пользователя по ID
     */
    @Test
    void getUserById_WithNonExistingId_ShouldThrowException() {
        // Given
        Long userId = 999L;

        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userController.getUserById(userId));

        assertEquals("User not found", exception.getMessage());
        verify(userService, times(1)).getUserById(userId);
        verify(userMapper, never()).toDto(any(User.class));
    }

    /**
     * Тест для endpoint: POST /admin/users
     * Назначение: Создание нового пользователя с паролем
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает созданного пользователя в формате DTO
     * - Вызывает маппер и сервис для создания пользователя
     */
    @Test
    void createUser_ShouldCreateAndReturnUser() {
        // Given
        UserDto inputDto = createUserDto(null, "newuser", "password123", "newuser@example.com", "New", "User", "USER", true);
        User userToCreate = createUser(null, "newuser", "newuser@example.com", "New", "User", "USER", true);
        User createdUser = createUser(1L, "newuser", "newuser@example.com", "New", "User", "USER", true);
        UserDto expectedDto = createUserDto(1L, "newuser", null, "newuser@example.com", "New", "User", "USER", true);

        when(userMapper.toEntity(inputDto)).thenReturn(userToCreate);
        when(userService.createUser(userToCreate)).thenReturn(createdUser);
        when(userMapper.toDto(createdUser)).thenReturn(expectedDto);

        // When
        ResponseEntity<UserDto> response = userController.createUser(inputDto);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedDto, response.getBody());
        verify(userMapper, times(1)).toEntity(inputDto);
        verify(userService, times(1)).createUser(userToCreate);
        verify(userMapper, times(1)).toDto(createdUser);
    }

    /**
     * Тест для endpoint: PUT /admin/users/{id}
     * Назначение: Обновление существующего пользователя с новым паролем
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает обновленного пользователя в формате DTO
     * - Вызывает маппер и сервис для обновления пользователя
     */
    @Test
    void updateUser_WithExistingId_ShouldUpdateAndReturnUser() {
        // Given
        Long userId = 1L;
        UserDto inputDto = createUserDto(null, "updateduser", "newpassword", "updated@example.com", "Updated", "User", "ADMIN", true);
        User userToUpdate = createUser(null, "updateduser", "updated@example.com", "Updated", "User", "ADMIN", true);
        User updatedUser = createUser(userId, "updateduser", "updated@example.com", "Updated", "User", "ADMIN", true);
        UserDto expectedDto = createUserDto(userId, "updateduser", null, "updated@example.com", "Updated", "User", "ADMIN", true);

        when(userMapper.toEntity(inputDto)).thenReturn(userToUpdate);
        when(userService.updateUser(userId, userToUpdate)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expectedDto);

        // When
        ResponseEntity<UserDto> response = userController.updateUser(userId, inputDto);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedDto, response.getBody());
        verify(userMapper, times(1)).toEntity(inputDto);
        verify(userService, times(1)).updateUser(userId, userToUpdate);
        verify(userMapper, times(1)).toDto(updatedUser);
    }

    /**
     * Тест для endpoint: PATCH /admin/users/{id}/deactivate
     * Назначение: Деактивация пользователя
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Тело ответа пустое
     * - Вызывает сервис для деактивации пользователя
     */
    @Test
    void deactivateUser_ShouldDeactivateUserAndReturnOk() {
        // Given
        Long userId = 1L;
        doNothing().when(userService).deactivateUser(userId);

        // When
        ResponseEntity<Void> response = userController.deactivateUser(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).deactivateUser(userId);
    }

    /**
     * Тест для endpoint: PATCH /admin/users/{id}/activate
     * Назначение: Активация пользователя
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Тело ответа пустое
     * - Вызывает сервис для активации пользователя
     */
    @Test
    void activateUser_ShouldActivateUserAndReturnOk() {
        // Given
        Long userId = 1L;
        doNothing().when(userService).activateUser(userId);

        // When
        ResponseEntity<Void> response = userController.activateUser(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).activateUser(userId);
    }

    /**
     * Тест для endpoint: DELETE /admin/users/{id}
     * Назначение: Удаление пользователя
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Тело ответа пустое
     * - Вызывает сервис для удаления пользователя
     */
    @Test
    void deleteUser_ShouldDeleteUserAndReturnOk() {
        // Given
        Long userId = 1L;
        doNothing().when(userService).deleteUser(userId);

        // When
        ResponseEntity<Void> response = userController.deleteUser(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).deleteUser(userId);
    }

    /**
     * Тест для endpoint: GET /admin/users (пустой список)
     * Назначение: Получение пустого списка пользователей
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает пустой список
     * - Корректно обрабатывает отсутствие пользователей
     */
    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        // Given
        List<User> emptyUsers = List.of();
        when(userService.getAllUsers()).thenReturn(emptyUsers);

        // When
        ResponseEntity<List<UserDto>> response = userController.getAllUsers();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(userService, times(1)).getAllUsers();
        verify(userMapper, never()).toDto(any(User.class));
    }

    /**
     * Тест для endpoint: PUT /admin/users/{id} с неактивным пользователем
     * Назначение: Обновление неактивного пользователя
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает обновленного пользователя
     * - Корректно обрабатывает разные статусы пользователей
     */
    @Test
    void updateUser_WithInactiveUser_ShouldUpdateAndReturnUser() {
        // Given
        Long userId = 1L;
        UserDto inputDto = createUserDto(null, "inactiveuser", "password", "inactive@example.com", "Inactive", "User", "USER", false);
        User userToUpdate = createUser(null, "inactiveuser", "inactive@example.com", "Inactive", "User", "USER", false);
        User updatedUser = createUser(userId, "inactiveuser", "inactive@example.com", "Inactive", "User", "USER", false);
        UserDto expectedDto = createUserDto(userId, "inactiveuser", null, "inactive@example.com", "Inactive", "User", "USER", false);

        when(userMapper.toEntity(inputDto)).thenReturn(userToUpdate);
        when(userService.updateUser(userId, userToUpdate)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expectedDto);

        // When
        ResponseEntity<UserDto> response = userController.updateUser(userId, inputDto);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedDto, response.getBody());
        verify(userMapper, times(1)).toEntity(inputDto);
        verify(userService, times(1)).updateUser(userId, userToUpdate);
        verify(userMapper, times(1)).toDto(updatedUser);
    }

    /**
     * Тест для endpoint: POST /admin/users с минимальными данными
     * Назначение: Создание пользователя с минимальным набором данных
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает созданного пользователя
     * - Корректно обрабатывает пользователя без дополнительных полей
     */
    @Test
    void createUser_WithMinimalData_ShouldCreateUser() {
        // Given
        UserDto inputDto = createUserDto(null, "minuser", "pass123", "min@example.com", null, null, "USER", true);
        User userToCreate = createUser(null, "minuser", "min@example.com", null, null, "USER", true);
        User createdUser = createUser(1L, "minuser", "min@example.com", null, null, "USER", true);
        UserDto expectedDto = createUserDto(1L, "minuser", null, "min@example.com", null, null, "USER", true);

        when(userMapper.toEntity(inputDto)).thenReturn(userToCreate);
        when(userService.createUser(userToCreate)).thenReturn(createdUser);
        when(userMapper.toDto(createdUser)).thenReturn(expectedDto);

        // When
        ResponseEntity<UserDto> response = userController.createUser(inputDto);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedDto, response.getBody());
        verify(userMapper, times(1)).toEntity(inputDto);
        verify(userService, times(1)).createUser(userToCreate);
        verify(userMapper, times(1)).toDto(createdUser);
    }

    /**
     * Тест для endpoint: POST /admin/users с ADMIN ролью
     * Назначение: Создание пользователя с ролью ADMIN
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает созданного пользователя
     * - Корректно обрабатывает разные роли пользователей
     */
    @Test
    void createUser_WithAdminRole_ShouldCreateAdminUser() {
        // Given
        UserDto inputDto = createUserDto(null, "adminuser", "adminpass", "admin@example.com", "Admin", "User", "ADMIN", true);
        User userToCreate = createUser(null, "adminuser", "admin@example.com", "Admin", "User", "ADMIN", true);
        User createdUser = createUser(1L, "adminuser", "admin@example.com", "Admin", "User", "ADMIN", true);
        UserDto expectedDto = createUserDto(1L, "adminuser", null, "admin@example.com", "Admin", "User", "ADMIN", true);

        when(userMapper.toEntity(inputDto)).thenReturn(userToCreate);
        when(userService.createUser(userToCreate)).thenReturn(createdUser);
        when(userMapper.toDto(createdUser)).thenReturn(expectedDto);

        // When
        ResponseEntity<UserDto> response = userController.createUser(inputDto);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedDto, response.getBody());
        verify(userMapper, times(1)).toEntity(inputDto);
        verify(userService, times(1)).createUser(userToCreate);
        verify(userMapper, times(1)).toDto(createdUser);
    }

    /**
     * Тест для endpoint: PATCH /admin/users/{id}/deactivate с несуществующим ID
     * Назначение: Попытка деактивации несуществующего пользователя
     * Ожидаемый результат:
     * - Выбрасывает исключение из сервиса (тестируется интеграция)
     * - Вызывает сервис для деактивации
     */
    @Test
    void deactivateUser_WithNonExistingId_ShouldPropagateException() {
        // Given
        Long userId = 999L;
        doThrow(new RuntimeException("User not found")).when(userService).deactivateUser(userId);

        // When & Then
        assertThrows(RuntimeException.class,
                () -> userController.deactivateUser(userId));

        verify(userService, times(1)).deactivateUser(userId);
    }

    /**
     * Тест для endpoint: PATCH /admin/users/{id}/activate с несуществующим ID
     * Назначение: Попытка активации несуществующего пользователя
     * Ожидаемый результат:
     * - Выбрасывает исключение из сервиса (тестируется интеграция)
     * - Вызывает сервис для активации
     */
    @Test
    void activateUser_WithNonExistingId_ShouldPropagateException() {
        // Given
        Long userId = 999L;
        doThrow(new RuntimeException("User not found")).when(userService).activateUser(userId);

        // When & Then
        assertThrows(RuntimeException.class,
                () -> userController.activateUser(userId));

        verify(userService, times(1)).activateUser(userId);
    }

    /**
     * Тест для endpoint: DELETE /admin/users/{id} с несуществующим ID
     * Назначение: Попытка удаления несуществующего пользователя
     * Ожидаемый результат:
     * - Выбрасывает исключение из сервиса (тестируется интеграция)
     * - Вызывает сервис для удаления
     */
    @Test
    void deleteUser_WithNonExistingId_ShouldPropagateException() {
        // Given
        Long userId = 999L;
        doThrow(new RuntimeException("User not found")).when(userService).deleteUser(userId);

        // When & Then
        assertThrows(RuntimeException.class,
                () -> userController.deleteUser(userId));

        verify(userService, times(1)).deleteUser(userId);
    }
}