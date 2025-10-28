package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.BookingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingSlotRepository extends JpaRepository<BookingSlot, Long> {

    @Query("SELECT COUNT(bs) > 0 FROM BookingSlot bs WHERE " +
            "bs.roomId = :roomId AND " +
            "bs.status IN ('RESERVED', 'CONFIRMED') AND " +
            "(:startDate < bs.endDate AND :endDate > bs.startDate)")
    boolean hasDateConflict(@Param("roomId") Long roomId,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    @Query("SELECT bs FROM BookingSlot bs WHERE " +
            "bs.roomId = :roomId AND " +
            "bs.status IN ('RESERVED', 'CONFIRMED') AND " +
            "(:startDate < bs.endDate AND :endDate > bs.startDate)")
    List<BookingSlot> findConflictingSlots(@Param("roomId") Long roomId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    List<BookingSlot> findByBookingId(Long bookingId);

    List<BookingSlot> findByStatusAndRoomId(String status, Long roomId);

    @Query("SELECT bs FROM BookingSlot bs WHERE bs.roomId = :roomId AND bs.status = 'RESERVED' AND bs.createdAt < :expiryTime")
    List<BookingSlot> findExpiredReservations(@Param("roomId") Long roomId,
                                              @Param("expiryTime") java.time.LocalDateTime expiryTime);
}