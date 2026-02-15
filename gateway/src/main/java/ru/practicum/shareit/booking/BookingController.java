package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingState;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
@Validated
public class BookingController {
    private final BookingClient bookingClient;

    @PostMapping
    public ResponseEntity<Object> createBooking(@Valid @RequestBody BookingCreateDto bookingDto,
                                                @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("POST /bookings - create booking for user ID: {}", userId);
        return bookingClient.createBooking(bookingDto, userId);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<Object> approveBooking(@PathVariable @Positive Long bookingId,
                                                 @RequestParam boolean approved,
                                                 @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("PATCH /bookings/{} - approve booking: {} by user ID: {}", bookingId, approved, userId);
        return bookingClient.approveBooking(bookingId, userId, approved);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Object> getBookingById(@PathVariable @Positive Long bookingId,
                                                 @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("GET /bookings/{} - get booking by user ID: {}", bookingId, userId);
        return bookingClient.getBookingById(bookingId, userId);
    }

    @GetMapping
    public ResponseEntity<Object> getUserBookings(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId,
            @RequestParam(defaultValue = "ALL") BookingState state,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /bookings - get user bookings for user ID: {}, state: {}", userId, state);
        return bookingClient.getUserBookings(userId, state, from, size);
    }

    @GetMapping("/owner")
    public ResponseEntity<Object> getOwnerBookings(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId,
            @RequestParam(defaultValue = "ALL") BookingState state,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /bookings/owner - get owner bookings for user ID: {}, state: {}", userId, state);
        return bookingClient.getOwnerBookings(userId, state, from, size);
    }
}