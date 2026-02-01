package ru.practicum.shareit.booking.model;

public enum BookingState {
    ALL,        // Все
    CURRENT,    // Текущие
    PAST,       // Завершенные
    FUTURE,     // Будущие
    WAITING,    // Ожидающие подтверждения
    REJECTED    // Отклоненные
}