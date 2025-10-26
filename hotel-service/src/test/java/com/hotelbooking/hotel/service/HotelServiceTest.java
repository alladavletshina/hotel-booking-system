package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.repository.HotelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelService hotelService;

    private Hotel createHotel(Long id, String name, String address, String description) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setDescription(description);
        return hotel;
    }

    /**
     * Тест для метода: findAll()
     * Назначение: Получение списка всех отелей
     * Ожидаемый результат:
     * - Возвращает список всех отелей из репозитория
     * - Вызывает метод findAll() репозитория один раз
     */
    @Test
    void findAll_ShouldReturnAllHotels() {
        // Given
        Hotel hotel1 = createHotel(1L, "Hotel A", "Address A", "Description A");
        Hotel hotel2 = createHotel(2L, "Hotel B", "Address B", "Description B");
        List<Hotel> expectedHotels = Arrays.asList(hotel1, hotel2);

        when(hotelRepository.findAll()).thenReturn(expectedHotels);

        // When
        List<Hotel> actualHotels = hotelService.findAll();

        // Then
        assertNotNull(actualHotels);
        assertEquals(2, actualHotels.size());
        assertEquals(expectedHotels, actualHotels);
        verify(hotelRepository, times(1)).findAll();
    }

    /**
     * Тест для метода: findById() с существующим ID
     * Назначение: Получение отеля по существующему идентификатору
     * Ожидаемый результат:
     * - Возвращает отель с указанным ID
     * - Вызывает метод findById() репозитория один раз
     */
    @Test
    void findById_WithExistingId_ShouldReturnHotel() {
        // Given
        Long hotelId = 1L;
        Hotel expectedHotel = createHotel(hotelId, "Test Hotel", "Test Address", "Test Description");

        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(expectedHotel));

        // When
        Hotel actualHotel = hotelService.findById(hotelId);

        // Then
        assertNotNull(actualHotel);
        assertEquals(expectedHotel.getId(), actualHotel.getId());
        assertEquals(expectedHotel.getName(), actualHotel.getName());
        assertEquals(expectedHotel.getAddress(), actualHotel.getAddress());
        verify(hotelRepository, times(1)).findById(hotelId);
    }

    /**
     * Тест для метода: findById() с несуществующим ID
     * Назначение: Попытка получения отеля по несуществующему идентификатору
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением об ошибке
     * - Вызывает метод findById() репозитория один раз
     */
    @Test
    void findById_WithNonExistingId_ShouldThrowException() {
        // Given
        Long nonExistingId = 999L;

        when(hotelRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> hotelService.findById(nonExistingId));

        assertEquals("Hotel not found with id: " + nonExistingId, exception.getMessage());
        verify(hotelRepository, times(1)).findById(nonExistingId);
    }

    /**
     * Тест для метода: save()
     * Назначение: Сохранение нового отеля
     * Ожидаемый результат:
     * - Возвращает сохраненный отель
     * - Вызывает метод save() репозитория один раз с правильным объектом
     */
    @Test
    void save_ShouldReturnSavedHotel() {
        // Given
        Hotel hotelToSave = createHotel(null, "New Hotel", "New Address", "New Description");
        Hotel savedHotel = createHotel(1L, "New Hotel", "New Address", "New Description");

        when(hotelRepository.save(hotelToSave)).thenReturn(savedHotel);

        // When
        Hotel result = hotelService.save(hotelToSave);

        // Then
        assertNotNull(result);
        assertEquals(savedHotel.getId(), result.getId());
        assertEquals(savedHotel.getName(), result.getName());
        assertEquals(savedHotel.getAddress(), result.getAddress());
        verify(hotelRepository, times(1)).save(hotelToSave);
    }

    /**
     * Тест для метода: update() с существующим ID
     * Назначение: Обновление данных существующего отеля
     * Ожидаемый результат:
     * - Возвращает обновленный отель
     * - Обновляет все поля отеля
     * - Вызывает методы findById() и save() репозитория
     */
    @Test
    void update_WithExistingId_ShouldReturnUpdatedHotel() {
        // Given
        Long hotelId = 1L;
        Hotel existingHotel = createHotel(hotelId, "Old Hotel", "Old Address", "Old Description");
        Hotel updatedDetails = createHotel(null, "Updated Hotel", "Updated Address", "Updated Description");
        Hotel expectedUpdatedHotel = createHotel(hotelId, "Updated Hotel", "Updated Address", "Updated Description");

        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(existingHotel));
        when(hotelRepository.save(existingHotel)).thenReturn(expectedUpdatedHotel);

        // When
        Hotel result = hotelService.update(hotelId, updatedDetails);

        // Then
        assertNotNull(result);
        assertEquals("Updated Hotel", result.getName());
        assertEquals("Updated Address", result.getAddress());
        assertEquals("Updated Description", result.getDescription());
        verify(hotelRepository, times(1)).findById(hotelId);
        verify(hotelRepository, times(1)).save(existingHotel);
    }

    /**
     * Тест для метода: update() с несуществующим ID
     * Назначение: Попытка обновления несуществующего отеля
     * Ожидаемый результат:
     * - Выбрасывает RuntimeException с сообщением об ошибке
     * - Не вызывает метод save() репозитория
     */
    @Test
    void update_WithNonExistingId_ShouldThrowException() {
        // Given
        Long nonExistingId = 999L;
        Hotel updatedDetails = createHotel(null, "Updated Hotel", "Updated Address", "Updated Description");

        when(hotelRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> hotelService.update(nonExistingId, updatedDetails));

        assertEquals("Hotel not found with id: " + nonExistingId, exception.getMessage());
        verify(hotelRepository, times(1)).findById(nonExistingId);
        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    /**
     * Тест для метода: deleteById()
     * Назначение: Удаление отеля по идентификатору
     * Ожидаемый результат:
     * - Вызывает метод deleteById() репозитория один раз с правильным ID
     * - Не выбрасывает исключений
     */
    @Test
    void deleteById_ShouldCallRepositoryDelete() {
        // Given
        Long hotelId = 1L;
        doNothing().when(hotelRepository).deleteById(hotelId);

        // When
        hotelService.deleteById(hotelId);

        // Then
        verify(hotelRepository, times(1)).deleteById(hotelId);
    }

    /**
     * Тест для метода: deleteById() с несуществующим ID
     * Назначение: Удаление отеля по несуществующему идентификатору
     * Ожидаемый результат:
     * - Вызывает метод deleteById() репозитория (Spring Data JPA не выбрасывает исключение при удалении несуществующей записи)
     * - Не выбрасывает исключений
     */
    @Test
    void deleteById_WithNonExistingId_ShouldNotThrowException() {
        // Given
        Long nonExistingId = 999L;
        doNothing().when(hotelRepository).deleteById(nonExistingId);

        // When & Then
        assertDoesNotThrow(() -> hotelService.deleteById(nonExistingId));
        verify(hotelRepository, times(1)).deleteById(nonExistingId);
    }
}