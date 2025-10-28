package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelService hotelService;

    private Hotel testHotel;
    private final Long HOTEL_ID = 1L;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setId(HOTEL_ID);
        testHotel.setName("Test Hotel");
        testHotel.setAddress("123 Test Street");
        testHotel.setDescription("A wonderful test hotel");
    }

    /**
     * Тест для метода: findAll
     * Назначение: Получение списка всех отелей
     * Сценарий: Успешное получение списка отелей
     * Ожидаемый результат:
     * - Возвращает список всех отелей
     * - Список содержит ожидаемое количество элементов
     * Бизнес-логика:
     * 1. Запрашивает все отели из репозитория
     * 2. Возвращает полученный список
     */
    @Test
    void findAll_ShouldReturnAllHotels() {
        // Arrange
        Hotel hotel1 = new Hotel();
        hotel1.setId(1L);
        hotel1.setName("Hotel 1");

        Hotel hotel2 = new Hotel();
        hotel2.setId(2L);
        hotel2.setName("Hotel 2");

        List<Hotel> expectedHotels = Arrays.asList(hotel1, hotel2);

        when(hotelRepository.findAll()).thenReturn(expectedHotels);

        // Act
        List<Hotel> result = hotelService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedHotels, result);

        verify(hotelRepository).findAll();
    }

    /**
     * Тест для метода: findAll
     * Назначение: Получение списка всех отелей
     * Сценарий: Пустая база данных отелей
     * Ожидаемый результат:
     * - Возвращает пустой список
     * - Не выбрасывает исключение
     * Бизнес-логика:
     * 1. Запрашивает все отели из репозитория
     * 2. Возвращает пустой список при отсутствии отелей
     */
    @Test
    void findAll_WhenNoHotels_ShouldReturnEmptyList() {
        // Arrange
        when(hotelRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<Hotel> result = hotelService.findAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(hotelRepository).findAll();
    }

    /**
     * Тест для метода: findById
     * Назначение: Получение отеля по ID
     * Сценарий: Успешное получение существующего отеля
     * Ожидаемый результат:
     * - Возвращает отель с указанным ID
     * - Отель содержит ожидаемые данные
     * Бизнес-логика:
     * 1. Ищет отель в репозитории по ID
     * 2. Возвращает найденный отель
     */
    @Test
    void findById_WithExistingId_ShouldReturnHotel() {
        // Arrange
        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(testHotel));

        // Act
        Hotel result = hotelService.findById(HOTEL_ID);

        // Assert
        assertNotNull(result);
        assertEquals(HOTEL_ID, result.getId());
        assertEquals("Test Hotel", result.getName());
        assertEquals("123 Test Street", result.getAddress());

        verify(hotelRepository).findById(HOTEL_ID);
    }

    /**
     * Тест для метода: findById
     * Назначение: Получение отеля по ID
     * Сценарий: Отель с указанным ID не найден
     * Ожидаемый результат:
     * - Выбрасывает исключение RuntimeException
     * - Сообщение об ошибке содержит ID отеля
     * Бизнес-логика:
     * 1. Ищет отель в репозитории по ID
     * 2. При отсутствии отеля выбрасывает исключение
     */
    @Test
    void findById_WithNonExistingId_ShouldThrowException() {
        // Arrange
        Long nonExistingId = 999L;
        when(hotelRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> hotelService.findById(nonExistingId));

        assertEquals("Hotel not found with id: " + nonExistingId, exception.getMessage());

        verify(hotelRepository).findById(nonExistingId);
    }

    /**
     * Тест для метода: save
     * Назначение: Сохранение нового отеля
     * Сценарий: Успешное сохранение отеля
     * Ожидаемый результат:
     * - Возвращает сохраненный отель
     * - Отель содержит переданные данные
     * Бизнес-логика:
     * 1. Принимает объект отеля для сохранения
     * 2. Сохраняет отель в репозитории
     * 3. Возвращает сохраненный отель
     */
    @Test
    void save_WithValidHotel_ShouldReturnSavedHotel() {
        // Arrange
        Hotel newHotel = new Hotel();
        newHotel.setName("New Hotel");
        newHotel.setAddress("456 New Street");
        newHotel.setDescription("A brand new hotel");

        when(hotelRepository.save(newHotel)).thenReturn(newHotel);

        // Act
        Hotel result = hotelService.save(newHotel);

        // Assert
        assertNotNull(result);
        assertEquals("New Hotel", result.getName());
        assertEquals("456 New Street", result.getAddress());

        verify(hotelRepository).save(newHotel);
    }

    /**
     * Тест для метода: save
     * Назначение: Сохранение нового отеля
     * Сценарий: Сохранение отеля с минимальными данными
     * Ожидаемый результат:
     * - Возвращает сохраненный отель
     * - Корректно обрабатывает отель с null описанием
     * Бизнес-логика:
     * 1. Принимает объект отеля с обязательными полями
     * 2. Сохраняет отель даже с неполными данными
     * 3. Возвращает сохраненный объект
     */
    @Test
    void save_WithMinimalData_ShouldReturnSavedHotel() {
        // Arrange
        Hotel minimalHotel = new Hotel();
        minimalHotel.setName("Minimal Hotel");
        minimalHotel.setAddress("Minimal Address");
        // description is null

        when(hotelRepository.save(minimalHotel)).thenReturn(minimalHotel);

        // Act
        Hotel result = hotelService.save(minimalHotel);

        // Assert
        assertNotNull(result);
        assertEquals("Minimal Hotel", result.getName());
        assertEquals("Minimal Address", result.getAddress());
        assertNull(result.getDescription());

        verify(hotelRepository).save(minimalHotel);
    }

    /**
     * Тест для метода: update
     * Назначение: Обновление данных отеля
     * Сценарий: Успешное обновление существующего отеля
     * Ожидаемый результат:
     * - Возвращает обновленный отель
     * - Данные отеля обновлены согласно переданным значениям
     * Бизнес-логика:
     * 1. Находит отель по ID
     * 2. Обновляет поля отеля из переданного объекта
     * 3. Сохраняет обновленный отель
     * 4. Возвращает сохраненный отель
     */
    @Test
    void update_WithExistingHotel_ShouldReturnUpdatedHotel() {
        // Arrange
        Hotel existingHotel = new Hotel();
        existingHotel.setId(HOTEL_ID);
        existingHotel.setName("Old Name");
        existingHotel.setAddress("Old Address");
        existingHotel.setDescription("Old Description");

        Hotel updateDetails = new Hotel();
        updateDetails.setName("Updated Name");
        updateDetails.setAddress("Updated Address");
        updateDetails.setDescription("Updated Description");

        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(existingHotel));
        when(hotelRepository.save(existingHotel)).thenReturn(existingHotel);

        // Act
        Hotel result = hotelService.update(HOTEL_ID, updateDetails);

        // Assert
        assertNotNull(result);
        assertEquals(HOTEL_ID, result.getId());
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Address", result.getAddress());
        assertEquals("Updated Description", result.getDescription());

        verify(hotelRepository).findById(HOTEL_ID);
        verify(hotelRepository).save(existingHotel);
    }

    /**
     * Тест для метода: update
     * Назначение: Обновление данных отеля
     * Сценарий: Обновление всех полей отеля, включая установку null значений
     * Ожидаемый результат:
     * - Возвращает обновленный отель
     * - Все поля обновляются согласно переданным значениям, даже если они null
     * Бизнес-логика:
     * 1. Находит отель по ID
     * 2. Обновляет все поля, включая установку null значений
     * 3. Сохраняет отель с обновленными данными
     */
    @Test
    void update_WithPartialData_ShouldUpdateAllFieldsIncludingNulls() {
        // Arrange
        Hotel existingHotel = new Hotel();
        existingHotel.setId(HOTEL_ID);
        existingHotel.setName("Original Name");
        existingHotel.setAddress("Original Address");
        existingHotel.setDescription("Original Description");

        Hotel updateDetails = new Hotel();
        updateDetails.setName("New Name Only");
        // address and description are null - они будут установлены в null

        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(existingHotel));
        when(hotelRepository.save(existingHotel)).thenReturn(existingHotel);

        // Act
        Hotel result = hotelService.update(HOTEL_ID, updateDetails);

        // Assert
        assertNotNull(result);
        assertEquals("New Name Only", result.getName());
        // Поскольку сервис устанавливает все поля из updateDetails, включая null
        assertNull(result.getAddress());
        assertNull(result.getDescription());

        verify(hotelRepository).findById(HOTEL_ID);
        verify(hotelRepository).save(existingHotel);
    }

    /**
     * Тест для метода: update
     * Назначение: Обновление данных отеля
     * Сценарий: Попытка обновления несуществующего отеля
     * Ожидаемый результат:
     * - Выбрасывает исключение RuntimeException
     * - Сообщение об ошибке указывает на отсутствие отеля
     * Бизнес-логика:
     * 1. Ищет отель по ID
     * 2. При отсутствии отеля выбрасывает исключение
     * 3. Не выполняет операцию сохранения
     */
    @Test
    void update_WithNonExistingHotel_ShouldThrowException() {
        // Arrange
        Long nonExistingId = 999L;
        Hotel updateDetails = new Hotel();
        updateDetails.setName("Updated Name");

        when(hotelRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> hotelService.update(nonExistingId, updateDetails));

        assertEquals("Hotel not found with id: " + nonExistingId, exception.getMessage());

        verify(hotelRepository).findById(nonExistingId);
        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    /**
     * Тест для метода: deleteById
     * Назначение: Удаление отеля по ID
     * Сценарий: Успешное удаление существующего отеля
     * Ожидаемый результат:
     * - Отель удаляется без исключений
     * - Вызывается метод удаления репозитория
     * Бизнес-логика:
     * 1. Вызывает метод удаления репозитория
     * 2. Не возвращает значение (void метод)
     * 3. Завершается без ошибок
     */
    @Test
    void deleteById_WithExistingId_ShouldDeleteHotel() {
        // Arrange
        doNothing().when(hotelRepository).deleteById(HOTEL_ID);

        // Act
        hotelService.deleteById(HOTEL_ID);

        // Assert
        verify(hotelRepository).deleteById(HOTEL_ID);
    }

    /**
     * Тест для метода: deleteById
     * Назначение: Удаление отеля по ID
     * Сценарий: Удаление несуществующего отеля
     * Ожидаемый результат:
     * - Не выбрасывает исключение при удалении несуществующего ID
     * - Вызывает метод удаления репозитория
     * Бизнес-логика:
     * 1. Вызывает метод удаления репозитория с любым ID
     * 2. Репозиторий обрабатывает несуществующие ID без ошибок
     * 3. Завершается нормально
     */
    @Test
    void deleteById_WithNonExistingId_ShouldCompleteWithoutError() {
        // Arrange
        Long nonExistingId = 999L;
        doNothing().when(hotelRepository).deleteById(nonExistingId);

        // Act & Assert
        assertDoesNotThrow(() -> hotelService.deleteById(nonExistingId));

        verify(hotelRepository).deleteById(nonExistingId);
    }

    /**
     * Тест для метода: deleteById
     * Назначение: Удаление отеля по ID
     * Сценарий: Удаление отеля с нулевым ID
     * Ожидаемый результат:
     * - Вызывает метод удаления с нулевым ID
     * - Завершается без исключений
     * Бизнес-логика:
     * 1. Принимает любой ID включая нулевой
     * 2. Делегирует удаление репозиторию
     * 3. Не выполняет валидацию ID
     */
    @Test
    void deleteById_WithNullId_ShouldCallRepository() {
        // Arrange
        Long nullId = 0L;
        doNothing().when(hotelRepository).deleteById(nullId);

        // Act & Assert
        assertDoesNotThrow(() -> hotelService.deleteById(nullId));

        verify(hotelRepository).deleteById(nullId);
    }
}