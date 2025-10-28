package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.statistics.HotelStatisticsDto;
import com.hotelbooking.hotel.dto.statistics.RoomPopularityDto;
import com.hotelbooking.hotel.dto.statistics.RoomTypeStatistics;
import com.hotelbooking.hotel.dto.statistics.DateRange;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.BookingSlot;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import com.hotelbooking.hotel.repository.BookingSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelStatisticsService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingSlotRepository bookingSlotRepository;

    public HotelStatisticsDto getHotelStatistics(Long hotelId, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating statistics for hotel {} from {} to {}", hotelId, startDate, endDate);

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        List<Room> hotelRooms = roomRepository.findByHotelId(hotelId);
        List<BookingSlot> bookingSlots = getBookingSlotsInPeriod(hotelId, startDate, endDate);

        HotelStatisticsDto statistics = new HotelStatisticsDto();
        statistics.setHotelId(hotelId);
        statistics.setHotelName(hotel.getName());
        statistics.setTotalRooms(hotelRooms.size());
        statistics.setAvailableRooms((int) hotelRooms.stream().filter(Room::getAvailable).count());
        statistics.setOccupancyRate(calculateOccupancyRate(hotelRooms, bookingSlots, startDate, endDate));
        statistics.setDailyOccupancy(calculateDailyOccupancy(hotelRooms, bookingSlots, startDate, endDate));
        statistics.setRoomTypeStats(calculateRoomTypeStatistics(hotelRooms, bookingSlots, startDate, endDate));
        statistics.setMostPopularRoom(findMostPopularRoom(hotelRooms));
        statistics.setLeastPopularRoom(findLeastPopularRoom(hotelRooms));
        statistics.setTotalRevenue(calculateTotalRevenue(bookingSlots, hotelRooms));
        statistics.setAverageRevenuePerRoom(calculateAverageRevenuePerRoom(statistics.getTotalRevenue(), hotelRooms.size()));
        statistics.setDateRange(new DateRange(startDate, endDate, ChronoUnit.DAYS.between(startDate, endDate) + 1));

        return statistics;
    }

    public List<HotelStatisticsDto> getHotelsComparison(LocalDate startDate, LocalDate endDate) {
        log.info("Calculating hotels comparison from {} to {}", startDate, endDate);

        return hotelRepository.findAll().stream()
                .map(hotel -> getHotelStatistics(hotel.getId(), startDate, endDate))
                .sorted(Comparator.comparingDouble(HotelStatisticsDto::getOccupancyRate).reversed())
                .collect(Collectors.toList());
    }

    public Map<LocalDate, Double> getDailyOccupancy(Long hotelId, LocalDate startDate, LocalDate endDate) {
        List<Room> hotelRooms = roomRepository.findByHotelId(hotelId);
        List<BookingSlot> bookingSlots = getBookingSlotsInPeriod(hotelId, startDate, endDate);

        return calculateDailyOccupancy(hotelRooms, bookingSlots, startDate, endDate);
    }

    public List<RoomPopularityDto> getPopularRooms(Long hotelId, Integer limit) {
        List<Room> hotelRooms = roomRepository.findByHotelId(hotelId);

        List<RoomPopularityDto> popularRooms = hotelRooms.stream()
                .sorted(Comparator.comparingInt(Room::getTimesBooked).reversed())
                .limit(limit)
                .map(this::convertToRoomPopularity)
                .collect(Collectors.toList());

        // Устанавливаем рейтинг популярности
        for (int i = 0; i < popularRooms.size(); i++) {
            popularRooms.get(i).setPopularityRank(i + 1);
        }

        return popularRooms;
    }

    // Вспомогательные методы
    private List<BookingSlot> getBookingSlotsInPeriod(Long hotelId, LocalDate startDate, LocalDate endDate) {
        List<Room> hotelRooms = roomRepository.findByHotelId(hotelId);
        List<Long> roomIds = hotelRooms.stream().map(Room::getId).collect(Collectors.toList());

        return roomIds.stream()
                .flatMap(roomId -> bookingSlotRepository.findConflictingSlots(roomId, startDate, endDate).stream())
                .filter(slot -> "CONFIRMED".equals(slot.getStatus()))
                .collect(Collectors.toList());
    }

    private Double calculateOccupancyRate(List<Room> rooms, List<BookingSlot> bookingSlots,
                                          LocalDate startDate, LocalDate endDate) {
        if (rooms.isEmpty()) return 0.0;

        long totalRoomDays = rooms.size() * ChronoUnit.DAYS.between(startDate, endDate.plusDays(1));
        long occupiedRoomDays = bookingSlots.stream()
                .mapToLong(slot -> ChronoUnit.DAYS.between(
                        slot.getStartDate().isAfter(startDate) ? slot.getStartDate() : startDate,
                        slot.getEndDate().isBefore(endDate) ? slot.getEndDate().plusDays(1) : endDate.plusDays(1)
                ))
                .sum();

        return totalRoomDays > 0 ? (occupiedRoomDays * 100.0) / totalRoomDays : 0.0;
    }

    private Map<LocalDate, Double> calculateDailyOccupancy(List<Room> rooms, List<BookingSlot> bookingSlots,
                                                           LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Double> dailyOccupancy = new LinkedHashMap<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            long occupiedRooms = bookingSlots.stream()
                    .filter(slot -> !currentDate.isBefore(slot.getStartDate()) && currentDate.isBefore(slot.getEndDate()))
                    .count();

            double occupancyRate = rooms.isEmpty() ? 0.0 : (occupiedRooms * 100.0) / rooms.size();
            dailyOccupancy.put(date, Math.round(occupancyRate * 100.0) / 100.0);
        }

        return dailyOccupancy;
    }

    private Map<String, RoomTypeStatistics> calculateRoomTypeStatistics(List<Room> rooms,
                                                                        List<BookingSlot> bookingSlots,
                                                                        LocalDate startDate, LocalDate endDate) {
        return rooms.stream()
                .collect(Collectors.groupingBy(
                        Room::getType,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                roomList -> {
                                    RoomTypeStatistics stats = new RoomTypeStatistics();
                                    stats.setRoomType(roomList.get(0).getType());
                                    stats.setRoomCount(roomList.size());

                                    List<BookingSlot> typeSlots = bookingSlots.stream()
                                            .filter(slot -> roomList.stream().anyMatch(room -> room.getId().equals(slot.getRoomId())))
                                            .collect(Collectors.toList());

                                    stats.setOccupancyRate(calculateOccupancyRate(roomList, typeSlots, startDate, endDate));
                                    stats.setTotalRevenue(calculateTotalRevenue(typeSlots, roomList));
                                    stats.setAverageBookings(roomList.stream()
                                            .mapToInt(Room::getTimesBooked)
                                            .average()
                                            .orElse(0.0));
                                    stats.setRevenuePerRoom(stats.getRoomCount() > 0 ?
                                            stats.getTotalRevenue() / stats.getRoomCount() : 0.0);

                                    return stats;
                                }
                        )
                ));
    }

    private Double calculateTotalRevenue(List<BookingSlot> bookingSlots, List<Room> rooms) {
        return bookingSlots.stream()
                .mapToDouble(slot -> {
                    Room room = rooms.stream()
                            .filter(r -> r.getId().equals(slot.getRoomId()))
                            .findFirst()
                            .orElse(null);
                    if (room != null && room.getPrice() != null) {
                        long days = ChronoUnit.DAYS.between(slot.getStartDate(), slot.getEndDate());
                        return room.getPrice() * days;
                    }
                    return 0.0;
                })
                .sum();
    }

    private Double calculateAverageRevenuePerRoom(Double totalRevenue, int roomCount) {
        return roomCount > 0 ? totalRevenue / roomCount : 0.0;
    }

    private RoomPopularityDto findMostPopularRoom(List<Room> rooms) {
        return rooms.stream()
                .max(Comparator.comparingInt(Room::getTimesBooked))
                .map(this::convertToRoomPopularity)
                .orElse(null);
    }

    private RoomPopularityDto findLeastPopularRoom(List<Room> rooms) {
        return rooms.stream()
                .min(Comparator.comparingInt(Room::getTimesBooked))
                .map(this::convertToRoomPopularity)
                .orElse(null);
    }

    private RoomPopularityDto convertToRoomPopularity(Room room) {
        RoomPopularityDto dto = new RoomPopularityDto();
        dto.setRoomId(room.getId());
        dto.setRoomNumber(room.getNumber());
        dto.setRoomType(room.getType());
        dto.setPrice(room.getPrice());
        dto.setTimesBooked(room.getTimesBooked());
        dto.setOccupancyRate(calculateRoomOccupancyRate(room));
        dto.setTotalRevenue(calculateRoomTotalRevenue(room));
        return dto;
    }

    private Double calculateRoomOccupancyRate(Room room) {
        // Упрощенный расчет загрузки для отдельного номера
        return room.getTimesBooked() * 2.5; // Пример: каждое бронирование = 2.5% загрузки
    }

    private Double calculateRoomTotalRevenue(Room room) {
        // Упрощенный расчет дохода для номера
        return room.getPrice() != null ? room.getPrice() * room.getTimesBooked() * 2.5 : 0.0;
    }
}