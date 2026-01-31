package ru.practicum.shareit.booking.model;

/**
 * Статус бронирования в базе данных
 * Хранится в таблице bookings в колонке "status"
 */
public enum BookingStatus {
    WAITING,    // Ожидает подтверждения владельца
    APPROVED,   // Подтверждено владельцем
    REJECTED,   // Отклонено владельцем
    CANCELED    // Отменено пользователем
}