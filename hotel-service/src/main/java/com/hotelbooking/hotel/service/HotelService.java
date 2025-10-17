package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;

    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }

    public Hotel findById(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + id));
    }

    public Hotel save(Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    public Hotel update(Long id, Hotel hotelDetails) {
        Hotel hotel = findById(id);
        hotel.setName(hotelDetails.getName());
        hotel.setAddress(hotelDetails.getAddress());
        hotel.setDescription(hotelDetails.getDescription());
        return hotelRepository.save(hotel);
    }

    public void deleteById(Long id) {
        hotelRepository.deleteById(id);
    }
}