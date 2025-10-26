package com.hotelbooking.hotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HotelControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HotelService hotelService;

    @Mock
    private HotelMapper hotelMapper;

    @InjectMocks
    private HotelController hotelController;

    private ObjectMapper objectMapper;
    private Hotel testHotel;
    private HotelDto testHotelDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(hotelController).build();
        objectMapper = new ObjectMapper();

        // Подготовка тестовых данных
        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("Test Hotel");
        testHotel.setAddress("Test Address");
        testHotel.setDescription("Test Description");

        testHotelDto = new HotelDto();
        testHotelDto.setId(1L);
        testHotelDto.setName("Test Hotel");
        testHotelDto.setAddress("Test Address");
        testHotelDto.setDescription("Test Description");
    }

    /**
     * Тест для endpoint: GET /hotels
     * Назначение: Получение списка всех отелей
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает список отелей в формате DTO
     */
    @Test
    void getAllHotels_ShouldReturnListOfHotels() throws Exception {
        // Arrange
        List<Hotel> hotels = Arrays.asList(testHotel);
        List<HotelDto> hotelDtos = Arrays.asList(testHotelDto);

        when(hotelService.findAll()).thenReturn(hotels);
        when(hotelMapper.toDto(testHotel)).thenReturn(testHotelDto);

        // Act & Assert
        mockMvc.perform(get("/hotels")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Test Hotel"))
                .andExpect(jsonPath("$[0].address").value("Test Address"));

        verify(hotelService, times(1)).findAll();
        verify(hotelMapper, times(1)).toDto(testHotel);
    }

    /**
     * Тест для endpoint: GET /hotels/{id}
     * Назначение: Получение отеля по ID
     * Сценарий: Отель существует
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает данные отеля
     */
    @Test
    void getHotel_ShouldReturnHotel_WhenHotelExists() throws Exception {
        // Arrange
        when(hotelService.findById(1L)).thenReturn(testHotel);

        // Act & Assert
        mockMvc.perform(get("/hotels/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Hotel"));

        verify(hotelService, times(1)).findById(1L);
    }

    /**
     * Тест для endpoint: GET /hotels/{id}
     * Назначение: Получение отеля по ID
     * Сценарий: Отель не найден
     * Ожидаемый результат:
     * - HTTP статус 404 (Not Found)
     * - Возвращает сообщение об ошибке
     */
    @Test
    void getHotel_ShouldReturnNotFound_WhenHotelDoesNotExist() throws Exception {
        // Arrange
        when(hotelService.findById(1L)).thenThrow(new RuntimeException("Hotel not found with id: 1"));

        // Act & Assert
        mockMvc.perform(get("/hotels/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Hotel not found with id: 1"));

        verify(hotelService, times(1)).findById(1L);
    }

    /**
     * Тест для endpoint: POST /hotels
     * Назначение: Создание нового отеля
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает созданный отель в формате DTO
     */
    @Test
    void createHotel_ShouldReturnCreatedHotel() throws Exception {
        // Arrange
        HotelDto requestDto = new HotelDto();
        requestDto.setName("New Hotel");
        requestDto.setAddress("New Address");
        requestDto.setDescription("New Description");

        Hotel savedHotel = new Hotel();
        savedHotel.setId(2L);
        savedHotel.setName("New Hotel");
        savedHotel.setAddress("New Address");
        savedHotel.setDescription("New Description");

        HotelDto responseDto = new HotelDto();
        responseDto.setId(2L);
        responseDto.setName("New Hotel");
        responseDto.setAddress("New Address");
        responseDto.setDescription("New Description");

        when(hotelMapper.toEntity(requestDto)).thenReturn(savedHotel);
        when(hotelService.save(any(Hotel.class))).thenReturn(savedHotel);
        when(hotelMapper.toDto(savedHotel)).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("New Hotel"))
                .andExpect(jsonPath("$.address").value("New Address"));

        verify(hotelMapper, times(1)).toEntity(requestDto);
        verify(hotelService, times(1)).save(savedHotel);
        verify(hotelMapper, times(1)).toDto(savedHotel);
    }

    /**
     * Тест для endpoint: PUT /hotels/{id}
     * Назначение: Обновление данных отеля
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Возвращает обновленный отель в формате DTO
     */
    @Test
    void updateHotel_ShouldReturnUpdatedHotel() throws Exception {
        // Arrange
        HotelDto updateDto = new HotelDto();
        updateDto.setName("Updated Hotel");
        updateDto.setAddress("Updated Address");
        updateDto.setDescription("Updated Description");

        Hotel updatedHotel = new Hotel();
        updatedHotel.setId(1L);
        updatedHotel.setName("Updated Hotel");
        updatedHotel.setAddress("Updated Address");
        updatedHotel.setDescription("Updated Description");

        HotelDto responseDto = new HotelDto();
        responseDto.setId(1L);
        responseDto.setName("Updated Hotel");
        responseDto.setAddress("Updated Address");
        responseDto.setDescription("Updated Description");

        when(hotelMapper.toEntity(updateDto)).thenReturn(updatedHotel);
        when(hotelService.update(eq(1L), any(Hotel.class))).thenReturn(updatedHotel);
        when(hotelMapper.toDto(updatedHotel)).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(put("/hotels/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Updated Hotel"))
                .andExpect(jsonPath("$.address").value("Updated Address"));

        verify(hotelMapper, times(1)).toEntity(updateDto);
        verify(hotelService, times(1)).update(eq(1L), any(Hotel.class));
        verify(hotelMapper, times(1)).toDto(updatedHotel);
    }

    /**
     * Тест для endpoint: DELETE /hotels/{id}
     * Назначение: Удаление отеля по ID
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Пустое тело ответа
     */
    @Test
    void deleteHotel_ShouldReturnOk_WhenHotelDeleted() throws Exception {
        // Arrange
        doNothing().when(hotelService).deleteById(1L);

        // Act & Assert
        mockMvc.perform(delete("/hotels/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(hotelService, times(1)).deleteById(1L);
    }

    /**
     * Тест для endpoint: GET /hotels
     * Назначение: Получение пустого списка отелей
     * Ожидаемый результат:
     * - HTTP статус 200 (OK)
     * - Пустой массив в ответе
     */
    @Test
    void getAllHotels_ShouldReturnEmptyList_WhenNoHotelsExist() throws Exception {
        // Arrange
        when(hotelService.findAll()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/hotels")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(hotelService, times(1)).findAll();
    }

    /**
     * Тест для endpoint: PUT /hotels/{id}
     * Назначение: Обновление несуществующего отеля
     * Сценарий: Отель не найден при обновлении
     * Ожидаемый результат:
     * - Исключение должно быть проброшено из сервиса
     */
    @Test
    void updateHotel_ShouldThrowException_WhenHotelNotFound() throws Exception {
        // Arrange
        HotelDto updateDto = new HotelDto();
        updateDto.setName("Non-existent Hotel");

        when(hotelMapper.toEntity(updateDto)).thenReturn(new Hotel());
        when(hotelService.update(eq(999L), any(Hotel.class)))
                .thenThrow(new RuntimeException("Hotel not found with id: 999"));

        // Act & Assert
        mockMvc.perform(put("/hotels/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());

        verify(hotelService, times(1)).update(eq(999L), any(Hotel.class));
    }
}