package com.hotelbooking.booking.entity;

public enum BookingStatus {
    PENDING,      // Ожидает подтверждения доступности
    CONFIRMED,    // Подтверждено
    CANCELLED,    // Отменено
    COMPLETED     // Завершено
}