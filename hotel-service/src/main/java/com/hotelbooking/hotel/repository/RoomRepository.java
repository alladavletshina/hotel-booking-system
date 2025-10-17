package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    List<Room> findByAvailableTrue();

    @Query("SELECT r FROM Room r WHERE r.available = true ORDER BY r.timesBooked ASC")
    List<Room> findAvailableRoomsOrderByTimesBooked();
}