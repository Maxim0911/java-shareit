package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingDto bookingDto, Long userId) {
        log.info("Creating booking for user ID: {}, item ID: {}", userId, bookingDto.getItemId());

        User booker = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Item item = itemRepository.findById(bookingDto.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + bookingDto.getItemId()));

        if (!item.getAvailable()) {
            throw new ValidationException("Item is not available for booking");
        }

        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Owner cannot book own item");
        }

        validateBookingDates(bookingDto.getStart(), bookingDto.getEnd());

        checkForOverlappingBookings(item.getId(), bookingDto.getStart(), bookingDto.getEnd());

        Booking booking = BookingMapper.toBooking(bookingDto);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created with ID: {}", savedBooking.getId());

        return BookingMapper.toBookingResponseDto(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponseDto approveBooking(Long bookingId, Long userId, boolean approved) {
        log.info("Approving booking ID: {} by user ID: {}, approved: {}", bookingId, userId, approved);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Only item owner can approve booking"); // ← Изменили здесь!
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Booking can only be approved from WAITING status");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);

        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking ID: {} status updated to: {}", bookingId, updatedBooking.getStatus());

        return BookingMapper.toBookingResponseDto(updatedBooking);
    }

    @Override
    public BookingResponseDto getBookingById(Long bookingId, Long userId) {
        log.info("Getting booking ID: {} for user ID: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getBooker().getId().equals(userId) &&
                !booking.getItem().getOwner().getId().equals(userId)) {
            throw new NotFoundException("Only booker or item owner can view booking");
        }

        return BookingMapper.toBookingResponseDto(booking);
    }

    @Override
    public List<BookingResponseDto> getUserBookings(Long userId, BookingState state, int from, int size) {
        log.info("Getting bookings for user ID: {}, state: {}", userId, state);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings;

        switch (state) {
            case ALL:
                bookings = bookingRepository.findAllByBookerIdOrderByStartDesc(userId, pageable);
                break;
            case CURRENT:
                bookings = bookingRepository.findCurrentByBookerId(userId, now, pageable);
                break;
            case PAST:
                bookings = bookingRepository.findAllByBookerIdAndEndBeforeOrderByStartDesc(userId, now, pageable);
                break;
            case FUTURE:
                bookings = bookingRepository.findAllByBookerIdAndStartAfterOrderByStartDesc(userId, now, pageable);
                break;
            case WAITING:
                bookings = bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING, pageable);
                break;
            case REJECTED:
                bookings = bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED, pageable);
                break;
            default:
                throw new ValidationException("Unknown state: " + state);
        }

        return bookings.stream()
                .map(BookingMapper::toBookingResponseDto)
                .collect(Collectors.toList());
    }

    private void checkForOverlappingBookings(Long itemId, LocalDateTime start, LocalDateTime end) {
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(itemId, start, end);

        if (!overlappingBookings.isEmpty()) {
            throw new ValidationException("Item is already booked for this time period");
        }
    }

    @Override
    public List<BookingResponseDto> getOwnerBookings(Long userId, BookingState state, int from, int size) {
        log.info("Getting bookings for owner ID: {}, state: {}", userId, state);

        List<Item> userItems = itemRepository.findAllByOwnerId(userId);
        if (userItems.isEmpty()) {
            throw new NotFoundException("User has no items"); // ← 404 статус
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings;

        switch (state) {
            case ALL:
                bookings = bookingRepository.findAllByItemOwnerIdOrderByStartDesc(userId, pageable);
                break;
            case CURRENT:
                bookings = bookingRepository.findCurrentByOwnerId(userId, now, pageable);
                break;
            case PAST:
                bookings = bookingRepository.findPastByOwnerId(userId, now, pageable);
                break;
            case FUTURE:
                bookings = bookingRepository.findFutureByOwnerId(userId, now, pageable);
                break;
            case WAITING:
                bookings = bookingRepository.findByOwnerIdAndStatus(userId, BookingStatus.WAITING, pageable);
                break;
            case REJECTED:
                bookings = bookingRepository.findByOwnerIdAndStatus(userId, BookingStatus.REJECTED, pageable);
                break;
            default:
                throw new ValidationException("Unknown state: " + state);
        }

        return bookings.stream()
                .map(BookingMapper::toBookingResponseDto)
                .collect(Collectors.toList());
    }

    private void validateBookingDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ValidationException("Start and end dates cannot be null");
        }

        if (start.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Start date cannot be in the past");
        }

        if (end.isBefore(start)) {
            throw new ValidationException("End date must be after start date");
        }

        if (start.isEqual(end)) {
            throw new ValidationException("Start and end dates cannot be equal");
        }
    }
}