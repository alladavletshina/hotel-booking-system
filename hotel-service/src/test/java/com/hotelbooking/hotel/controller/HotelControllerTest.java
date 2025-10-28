package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.HotelDto;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.mapper.HotelMapper;
import com.hotelbooking.hotel.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelControllerTest {

    @Mock
    private HotelService hotelService;

    @Mock
    private HotelMapper hotelMapper;

    @InjectMocks
    private HotelController hotelController;

    private Hotel testHotel;
    private HotelDto testHotelDto;
    private final Long HOTEL_ID = 1L;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setId(HOTEL_ID);
        testHotel.setName("Test Hotel");
        testHotel.setAddress("123 Test Street");
        testHotel.setDescription("A wonderful test hotel");

        testHotelDto = new HotelDto();
        testHotelDto.setId(HOTEL_ID);
        testHotelDto.setName("Test Hotel");
        testHotelDto.setAddress("123 Test Street");
        testHotelDto.setDescription("A wonderful test hotel");
    }

    /**
     * Тест для endpoint: GET /hotels
     * Назначение: Получение всех отелей
     * Сценарий: Успешное получение списка отелей пользователем
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает список отелей в DTO формате
     * Бизнес-логика:
     * 1. Проверяет права доступа (USER или ADMIN)
     * 2. Получает все отели из сервиса
     * 3. Преобразует список сущностей в список DTO
     * 4. Возвращает результат
     */
    @Test
    void getAllHotels_WithUserRole_ShouldReturnHotelsList() {

        setupUserAuthentication("ROLE_USER");
        List<Hotel> hotels = Collections.singletonList(testHotel);

        when(hotelService.findAll()).thenReturn(hotels);
        when(hotelMapper.toDto(testHotel)).thenReturn(testHotelDto);


        ResponseEntity<List<HotelDto>> response = hotelController.getAllHotels();


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(HOTEL_ID, response.getBody().get(0).getId());

        verify(hotelService).findAll();
        verify(hotelMapper).toDto(testHotel);
    }

    /**
     * Тест для endpoint: GET /hotels
     * Назначение: Получение всех отелей
     * Сценарий: Успешное получение списка отелей администратором
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Администратор имеет доступ к списку отелей
     * Бизнес-логика:
     * 1. Проверяет роль ADMIN у текущего пользователя
     * 2. Разрешает доступ к списку отелей
     * 3. Возвращает список отелей
     */
    @Test
    void getAllHotels_WithAdminRole_ShouldReturnHotelsList() {
        // Arrange
        setupUserAuthentication("ROLE_ADMIN");
        List<Hotel> hotels = Collections.singletonList(testHotel);

        when(hotelService.findAll()).thenReturn(hotels);
        when(hotelMapper.toDto(testHotel)).thenReturn(testHotelDto);


        ResponseEntity<List<HotelDto>> response = hotelController.getAllHotels();


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(hotelService).findAll();
    }

    /**
     * Тест для endpoint: GET /hotels/{id}
     * Назначение: Получение отеля по ID
     * Сценарий: Успешное получение существующего отеля
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает отель в формате сущности
     * Бизнес-логика:
     * 1. Проверяет права доступа (USER или ADMIN)
     * 2. Получает отель по ID из сервиса
     * 3. Возвращает отель в формате сущности (не DTO)
     */
    @Test
    void getHotel_WithExistingId_ShouldReturnHotel() {

        setupUserAuthentication("ROLE_USER");
        when(hotelService.findById(HOTEL_ID)).thenReturn(testHotel);


        ResponseEntity<?> response = hotelController.getHotel(HOTEL_ID);


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testHotel, response.getBody());

        verify(hotelService).findById(HOTEL_ID);
    }

    /**
     * Тест для endpoint: GET /hotels/{id}
     * Назначение: Получение отеля по ID
     * Сценарий: Отель с указанным ID не найден
     * Ожидаемый результат:
     * - Возвращает статус 404 NOT FOUND
     * - Возвращает сообщение об ошибке
     * Бизнес-логика:
     * 1. Ищет отель по ID в сервисе
     * 2. При отсутствии отеля возвращает 404 с сообщением
     */
    @Test
    void getHotel_WithNonExistingId_ShouldReturnNotFound() {

        setupUserAuthentication("ROLE_USER");
        String errorMessage = "Hotel not found with id: " + HOTEL_ID;
        when(hotelService.findById(HOTEL_ID)).thenThrow(new RuntimeException(errorMessage));


        ResponseEntity<?> response = hotelController.getHotel(HOTEL_ID);


        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());

        verify(hotelService).findById(HOTEL_ID);
    }

    /**
     * Тест для endpoint: POST /hotels
     * Назначение: Создание нового отеля
     * Сценарий: Успешное создание отеля администратором
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает созданный отель в DTO формате
     * Бизнес-логика:
     * 1. Проверяет роль ADMIN у текущего пользователя
     * 2. Преобразует DTO в сущность
     * 3. Сохраняет отель через сервис
     * 4. Преобразует результат в DTO и возвращает
     */
    @Test
    void createHotel_WithAdminRole_ShouldCreateHotel() {

        setupUserAuthentication("ROLE_ADMIN");
        when(hotelMapper.toEntity(testHotelDto)).thenReturn(testHotel);
        when(hotelService.save(testHotel)).thenReturn(testHotel);
        when(hotelMapper.toDto(testHotel)).thenReturn(testHotelDto);


        ResponseEntity<HotelDto> response = hotelController.createHotel(testHotelDto);


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HOTEL_ID, response.getBody().getId());

        verify(hotelMapper).toEntity(testHotelDto);
        verify(hotelService).save(testHotel);
        verify(hotelMapper).toDto(testHotel);
    }

    /**
     * Тест для endpoint: PUT /hotels/{id}
     * Назначение: Обновление данных отеля
     * Сценарий: Успешное обновление существующего отеля администратором
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает обновленный отель в DTO формате
     * Бизнес-логика:
     * 1. Проверяет роль ADMIN у текущего пользователя
     * 2. Преобразует DTO в сущность
     * 3. Обновляет отель через сервис
     * 4. Преобразует результат в DTO и возвращает
     */
    @Test
    void updateHotel_WithExistingHotel_ShouldUpdateHotel() {

        setupUserAuthentication("ROLE_ADMIN");
        when(hotelMapper.toEntity(testHotelDto)).thenReturn(testHotel);
        when(hotelService.update(HOTEL_ID, testHotel)).thenReturn(testHotel);
        when(hotelMapper.toDto(testHotel)).thenReturn(testHotelDto);


        ResponseEntity<?> response = hotelController.updateHotel(HOTEL_ID, testHotelDto);


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HOTEL_ID, ((HotelDto) response.getBody()).getId());

        verify(hotelMapper).toEntity(testHotelDto);
        verify(hotelService).update(HOTEL_ID, testHotel);
        verify(hotelMapper).toDto(testHotel);
    }

    /**
     * Тест для endpoint: PUT /hotels/{id}
     * Назначение: Обновление данных отеля
     * Сценарий: Попытка обновления несуществующего отеля
     * Ожидаемый результат:
     * - Возвращает статус 404 NOT FOUND
     * - Возвращает сообщение об ошибке
     * Бизнес-логика:
     * 1. Пытается обновить отель по ID
     * 2. При отсутствии отеля возвращает 404 с сообщением
     */
    @Test
    void updateHotel_WithNonExistingHotel_ShouldReturnNotFound() {

        setupUserAuthentication("ROLE_ADMIN");
        String errorMessage = "Hotel not found with id: " + HOTEL_ID;
        when(hotelMapper.toEntity(testHotelDto)).thenReturn(testHotel);
        when(hotelService.update(HOTEL_ID, testHotel)).thenThrow(new RuntimeException(errorMessage));


        ResponseEntity<?> response = hotelController.updateHotel(HOTEL_ID, testHotelDto);


        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());

        verify(hotelMapper).toEntity(testHotelDto);
        verify(hotelService).update(HOTEL_ID, testHotel);
        verify(hotelMapper, never()).toDto(any(Hotel.class));
    }

    /**
     * Тест для endpoint: DELETE /hotels/{id}
     * Назначение: Удаление отеля по ID
     * Сценарий: Успешное удаление отеля администратором
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Тело ответа пустое
     * Бизнес-логика:
     * 1. Проверяет роль ADMIN у текущего пользователя
     * 2. Вызывает сервис для удаления отеля
     * 3. Возвращает успешный статус без тела
     */
    @Test
    void deleteHotel_WithAdminRole_ShouldDeleteHotel() {

        setupUserAuthentication("ROLE_ADMIN");
        doNothing().when(hotelService).deleteById(HOTEL_ID);


        ResponseEntity<Void> response = hotelController.deleteHotel(HOTEL_ID);


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(hotelService).deleteById(HOTEL_ID);
    }

    /**
     * Тест для endpoint: GET /hotels
     * Назначение: Получение всех отелей
     * Сценарий: Пустой список отелей
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает пустой список
     * Бизнес-логика:
     * 1. Обрабатывает случай отсутствия отелей
     * 2. Возвращает корректный пустой ответ
     */
    @Test
    void getAllHotels_WithEmptyList_ShouldReturnEmptyList() {

        setupUserAuthentication("ROLE_USER");
        when(hotelService.findAll()).thenReturn(Collections.emptyList());


        ResponseEntity<List<HotelDto>> response = hotelController.getAllHotels();


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(hotelService).findAll();
        verify(hotelMapper, never()).toDto(any(Hotel.class));
    }

    /**
     * Тест для endpoint: POST /hotels
     * Назначение: Создание нового отеля
     * Сценарий: Создание отеля с минимальными данными
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Корректно обрабатывает отель с неполными данными
     * Бизнес-логика:
     * 1. Принимает DTO с обязательными полями
     * 2. Сохраняет отель даже с неполными данными
     * 3. Возвращает сохраненный отель
     */
    @Test
    void createHotel_WithMinimalData_ShouldCreateHotel() {

        setupUserAuthentication("ROLE_ADMIN");
        HotelDto minimalDto = new HotelDto();
        minimalDto.setName("Minimal Hotel");
        minimalDto.setAddress("Minimal Address");

        Hotel minimalHotel = new Hotel();
        minimalHotel.setName("Minimal Hotel");
        minimalHotel.setAddress("Minimal Address");

        Hotel savedHotel = new Hotel();
        savedHotel.setId(2L);
        savedHotel.setName("Minimal Hotel");
        savedHotel.setAddress("Minimal Address");

        HotelDto savedDto = new HotelDto();
        savedDto.setId(2L);
        savedDto.setName("Minimal Hotel");
        savedDto.setAddress("Minimal Address");

        when(hotelMapper.toEntity(minimalDto)).thenReturn(minimalHotel);
        when(hotelService.save(minimalHotel)).thenReturn(savedHotel);
        when(hotelMapper.toDto(savedHotel)).thenReturn(savedDto);


        ResponseEntity<HotelDto> response = hotelController.createHotel(minimalDto);


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2L, response.getBody().getId());
        assertEquals("Minimal Hotel", response.getBody().getName());

        verify(hotelMapper).toEntity(minimalDto);
        verify(hotelService).save(minimalHotel);
        verify(hotelMapper).toDto(savedHotel);
    }

    /**
     * Тест для endpoint: GET /hotels/{id}
     * Назначение: Получение отеля по ID
     * Сценарий: Получение отеля с полными данными
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Возвращает отель со всеми заполненными полями
     * Бизнес-логика:
     * 1. Получает отель по ID
     * 2. Возвращает полную информацию об отеле
     */
    @Test
    void getHotel_WithFullHotelData_ShouldReturnCompleteHotel() {

        setupUserAuthentication("ROLE_USER");
        Hotel fullHotel = new Hotel();
        fullHotel.setId(HOTEL_ID);
        fullHotel.setName("Luxury Hotel");
        fullHotel.setAddress("456 Luxury Avenue");
        fullHotel.setDescription("A 5-star luxury hotel with all amenities");

        when(hotelService.findById(HOTEL_ID)).thenReturn(fullHotel);


        ResponseEntity<?> response = hotelController.getHotel(HOTEL_ID);


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Hotel returnedHotel = (Hotel) response.getBody();
        assertEquals("Luxury Hotel", returnedHotel.getName());
        assertEquals("456 Luxury Avenue", returnedHotel.getAddress());
        assertEquals("A 5-star luxury hotel with all amenities", returnedHotel.getDescription());

        verify(hotelService).findById(HOTEL_ID);
    }

    /**
     * Тест для endpoint: PUT /hotels/{id}
     * Назначение: Обновление данных отеля
     * Сценарий: Частичное обновление данных отеля
     * Ожидаемый результат:
     * - Возвращает статус 200 OK
     * - Обновляет только указанные поля отеля
     * Бизнес-логика:
     * 1. Принимает DTO с частичными данными
     * 2. Обновляет отель через сервис
     * 3. Возвращает обновленный отель
     */
    @Test
    void updateHotel_WithPartialData_ShouldUpdateHotel() {

        setupUserAuthentication("ROLE_ADMIN");
        HotelDto partialDto = new HotelDto();
        partialDto.setName("Updated Name Only");


        Hotel partialHotel = new Hotel();
        partialHotel.setName("Updated Name Only");

        Hotel updatedHotel = new Hotel();
        updatedHotel.setId(HOTEL_ID);
        updatedHotel.setName("Updated Name Only");
        updatedHotel.setAddress("123 Test Street"); // Original address preserved
        updatedHotel.setDescription("A wonderful test hotel"); // Original description preserved

        HotelDto updatedDto = new HotelDto();
        updatedDto.setId(HOTEL_ID);
        updatedDto.setName("Updated Name Only");
        updatedDto.setAddress("123 Test Street");
        updatedDto.setDescription("A wonderful test hotel");

        when(hotelMapper.toEntity(partialDto)).thenReturn(partialHotel);
        when(hotelService.update(HOTEL_ID, partialHotel)).thenReturn(updatedHotel);
        when(hotelMapper.toDto(updatedHotel)).thenReturn(updatedDto);


        ResponseEntity<?> response = hotelController.updateHotel(HOTEL_ID, partialDto);


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        HotelDto resultDto = (HotelDto) response.getBody();
        assertEquals("Updated Name Only", resultDto.getName());
        assertEquals("123 Test Street", resultDto.getAddress());

        verify(hotelMapper).toEntity(partialDto);
        verify(hotelService).update(HOTEL_ID, partialHotel);
        verify(hotelMapper).toDto(updatedHotel);
    }

    private void setupUserAuthentication(String role) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "testUser",
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}