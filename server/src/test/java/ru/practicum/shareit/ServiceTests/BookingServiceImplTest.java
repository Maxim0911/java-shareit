package ru.practicum.shareit.ServiceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingServiceImpl;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Captor
    private ArgumentCaptor<Booking> bookingCaptor;

    private User owner;
    private User booker;
    private Item item;
    private Booking booking;
    private BookingDto bookingDto;
    private LocalDateTime now;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        start = now.plusDays(1);
        end = now.plusDays(2);

        owner = new User();
        owner.setId(1L);
        owner.setName("Owner");
        owner.setEmail("owner@test.com");

        booker = new User();
        booker.setId(2L);
        booker.setName("Booker");
        booker.setEmail("booker@test.com");

        item = new Item();
        item.setId(1L);
        item.setName("Дрель");
        item.setDescription("Электрическая дрель");
        item.setAvailable(true);
        item.setOwner(owner);

        booking = new Booking();
        booking.setId(1L);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        bookingDto = BookingDto.builder()
                .itemId(1L)
                .start(start)
                .end(end)
                .build();
    }

    // ==================== Тесты createBooking ====================

    @Test
    void createBooking_ItemAvailable_ShouldSuccess() {
        // given
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // when
        BookingResponseDto result = bookingService.createBooking(bookingDto, booker.getId());

        // then
        assertNotNull(result);
        assertEquals(booking.getId(), result.getId());
        assertEquals(booking.getStart(), result.getStart());
        assertEquals(booking.getEnd(), result.getEnd());
        assertEquals(booking.getStatus(), result.getStatus());

        verify(bookingRepository).save(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(item, savedBooking.getItem());
        assertEquals(booker, savedBooking.getBooker());
        assertEquals(BookingStatus.WAITING, savedBooking.getStatus());
    }

    @Test
    void createBooking_UserNotFound_ShouldThrowException() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.createBooking(bookingDto, 999L));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ItemNotFound_ShouldThrowException() {
        // given
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.createBooking(bookingDto, booker.getId()));

        assertEquals("Item not found with id: 1", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ItemNotAvailable_ShouldThrowException() {
        // given
        item.setAvailable(false);
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bookingService.createBooking(bookingDto, booker.getId()));

        assertEquals("Item is not available for booking", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_OwnerBookOwnItem_ShouldThrowException() {
        // given
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.createBooking(bookingDto, owner.getId()));

        assertEquals("Owner cannot book own item", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_StartDateInPast_ShouldThrowException() {
        // given
        bookingDto.setStart(now.minusDays(1));
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bookingService.createBooking(bookingDto, booker.getId()));

        assertEquals("Start date cannot be in the past", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_EndDateBeforeStart_ShouldThrowException() {
        // given
        bookingDto.setStart(now.plusDays(2));
        bookingDto.setEnd(now.plusDays(1));
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bookingService.createBooking(bookingDto, booker.getId()));

        assertEquals("End date must be after start date", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_StartEqualsEnd_ShouldThrowException() {
        // given
        LocalDateTime sameTime = now.plusDays(1);
        bookingDto.setStart(sameTime);
        bookingDto.setEnd(sameTime);
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bookingService.createBooking(bookingDto, booker.getId()));

        assertEquals("Start and end dates cannot be equal", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_StartNull_ShouldThrowException() {
        // given
        bookingDto.setStart(null);
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bookingService.createBooking(bookingDto, booker.getId()));

        assertEquals("Start and end dates cannot be null", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_WithOverlappingBookings_ShouldThrowException() {
        // given
        Booking overlappingBooking = new Booking();
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(bookingRepository.findOverlappingBookings(eq(item.getId()), any(), any()))
                .thenReturn(List.of(overlappingBooking));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bookingService.createBooking(bookingDto, booker.getId()));

        assertEquals("Item is already booked for this time period", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    // ==================== Тесты approveBooking ====================

    @Test
    void approveBooking_Owner_Approve_ShouldSetApproved() {
        // given
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // when
        BookingResponseDto result = bookingService.approveBooking(booking.getId(), owner.getId(), true);

        // then
        assertNotNull(result);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking updatedBooking = bookingCaptor.getValue();
        assertEquals(BookingStatus.APPROVED, updatedBooking.getStatus());
    }

    @Test
    void approveBooking_Owner_Reject_ShouldSetRejected() {
        // given
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // when
        BookingResponseDto result = bookingService.approveBooking(booking.getId(), owner.getId(), false);

        // then
        assertNotNull(result);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking updatedBooking = bookingCaptor.getValue();
        assertEquals(BookingStatus.REJECTED, updatedBooking.getStatus());
    }

    @Test
    void approveBooking_NotOwner_ShouldThrowException() {
        // given
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // when & then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> bookingService.approveBooking(booking.getId(), booker.getId(), true));

        assertEquals("Only item owner can approve booking", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approveBooking_BookingNotFound_ShouldThrowException() {
        // given
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.approveBooking(999L, owner.getId(), true));

        assertEquals("Booking not found with id: 999", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approveBooking_AlreadyApproved_ShouldThrowException() {
        // given
        booking.setStatus(BookingStatus.APPROVED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bookingService.approveBooking(booking.getId(), owner.getId(), true));

        assertEquals("Booking can only be approved from WAITING status", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approveBooking_AlreadyRejected_ShouldThrowException() {
        // given
        booking.setStatus(BookingStatus.REJECTED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bookingService.approveBooking(booking.getId(), owner.getId(), true));

        assertEquals("Booking can only be approved from WAITING status", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    // ==================== Тесты getBookingById ====================

    @Test
    void getBookingById_AsBooker_ShouldReturnBooking() {
        // given
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // when
        BookingResponseDto result = bookingService.getBookingById(booking.getId(), booker.getId());

        // then
        assertNotNull(result);
        assertEquals(booking.getId(), result.getId());
    }

    @Test
    void getBookingById_AsOwner_ShouldReturnBooking() {
        // given
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // when
        BookingResponseDto result = bookingService.getBookingById(booking.getId(), owner.getId());

        // then
        assertNotNull(result);
        assertEquals(booking.getId(), result.getId());
    }

    @Test
    void getBookingById_AsOtherUser_ShouldThrowException() {
        // given
        User otherUser = new User();
        otherUser.setId(3L);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.getBookingById(booking.getId(), 3L));

        assertEquals("Only booker or item owner can view booking", exception.getMessage());
    }

    @Test
    void getBookingById_BookingNotFound_ShouldThrowException() {
        // given
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.getBookingById(999L, booker.getId()));

        assertEquals("Booking not found with id: 999", exception.getMessage());
    }

    // ==================== Тесты getUserBookings ====================

    @Test
    void getUserBookings_WithStateAll_ShouldReturnAll() {
        // given
        List<Booking> bookings = List.of(booking);
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdOrderByStartDesc(eq(booker.getId()), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getUserBookings(booker.getId(), BookingState.ALL, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findAllByBookerIdOrderByStartDesc(eq(booker.getId()), any(Pageable.class));
    }

    @Test
    void getUserBookings_WithStateCurrent_ShouldReturnCurrent() {
        // given
        List<Booking> bookings = List.of(booking);
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(bookingRepository.findCurrentByBookerId(eq(booker.getId()), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getUserBookings(booker.getId(), BookingState.CURRENT, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findCurrentByBookerId(eq(booker.getId()), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void getUserBookings_WithStatePast_ShouldReturnPast() {
        // given
        List<Booking> bookings = List.of(booking);
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndEndBeforeOrderByStartDesc(
                eq(booker.getId()), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getUserBookings(booker.getId(), BookingState.PAST, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findAllByBookerIdAndEndBeforeOrderByStartDesc(
                eq(booker.getId()), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void getUserBookings_WithStateFuture_ShouldReturnFuture() {
        // given
        List<Booking> bookings = List.of(booking);
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStartAfterOrderByStartDesc(
                eq(booker.getId()), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getUserBookings(booker.getId(), BookingState.FUTURE, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findAllByBookerIdAndStartAfterOrderByStartDesc(
                eq(booker.getId()), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void getUserBookings_WithStateWaiting_ShouldReturnWaiting() {
        // given
        List<Booking> bookings = List.of(booking);
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(
                eq(booker.getId()), eq(BookingStatus.WAITING), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getUserBookings(booker.getId(), BookingState.WAITING, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findAllByBookerIdAndStatusOrderByStartDesc(
                eq(booker.getId()), eq(BookingStatus.WAITING), any(Pageable.class));
    }

    @Test
    void getUserBookings_WithStateRejected_ShouldReturnRejected() {
        // given
        List<Booking> bookings = List.of(booking);
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(
                eq(booker.getId()), eq(BookingStatus.REJECTED), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getUserBookings(booker.getId(), BookingState.REJECTED, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findAllByBookerIdAndStatusOrderByStartDesc(
                eq(booker.getId()), eq(BookingStatus.REJECTED), any(Pageable.class));
    }

    @Test
    void getUserBookings_UserNotFound_ShouldThrowException() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.getUserBookings(999L, BookingState.ALL, 0, 10));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(bookingRepository, never()).findAllByBookerIdOrderByStartDesc(anyLong(), any());
    }

    @Test
    void getUserBookings_WithInvalidState_ShouldThrowException() {
        // given
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        BookingState invalidState = null;

        // when & then
        assertThrows(NullPointerException.class,
                () -> bookingService.getUserBookings(booker.getId(), invalidState, 0, 10));
    }

    // ==================== Тесты getOwnerBookings ====================

    @Test
    void getOwnerBookings_WithStateAll_ShouldReturnAll() {
        // given
        List<Item> items = List.of(item);
        List<Booking> bookings = List.of(booking);
        when(itemRepository.findAllByOwnerId(owner.getId())).thenReturn(items);
        when(bookingRepository.findAllByItemOwnerIdOrderByStartDesc(eq(owner.getId()), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getOwnerBookings(owner.getId(), BookingState.ALL, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findAllByItemOwnerIdOrderByStartDesc(eq(owner.getId()), any(Pageable.class));
    }

    @Test
    void getOwnerBookings_WithStateCurrent_ShouldReturnCurrent() {
        // given
        List<Item> items = List.of(item);
        List<Booking> bookings = List.of(booking);
        when(itemRepository.findAllByOwnerId(owner.getId())).thenReturn(items);
        when(bookingRepository.findCurrentByOwnerId(eq(owner.getId()), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getOwnerBookings(owner.getId(), BookingState.CURRENT, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findCurrentByOwnerId(eq(owner.getId()), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void getOwnerBookings_WithStatePast_ShouldReturnPast() {
        // given
        List<Item> items = List.of(item);
        List<Booking> bookings = List.of(booking);
        when(itemRepository.findAllByOwnerId(owner.getId())).thenReturn(items);
        when(bookingRepository.findPastByOwnerId(eq(owner.getId()), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getOwnerBookings(owner.getId(), BookingState.PAST, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findPastByOwnerId(eq(owner.getId()), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void getOwnerBookings_WithStateFuture_ShouldReturnFuture() {
        // given
        List<Item> items = List.of(item);
        List<Booking> bookings = List.of(booking);
        when(itemRepository.findAllByOwnerId(owner.getId())).thenReturn(items);
        when(bookingRepository.findFutureByOwnerId(eq(owner.getId()), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getOwnerBookings(owner.getId(), BookingState.FUTURE, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findFutureByOwnerId(eq(owner.getId()), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void getOwnerBookings_WithStateWaiting_ShouldReturnWaiting() {
        // given
        List<Item> items = List.of(item);
        List<Booking> bookings = List.of(booking);
        when(itemRepository.findAllByOwnerId(owner.getId())).thenReturn(items);
        when(bookingRepository.findByOwnerIdAndStatus(eq(owner.getId()), eq(BookingStatus.WAITING), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getOwnerBookings(owner.getId(), BookingState.WAITING, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findByOwnerIdAndStatus(eq(owner.getId()), eq(BookingStatus.WAITING), any(Pageable.class));
    }

    @Test
    void getOwnerBookings_WithStateRejected_ShouldReturnRejected() {
        // given
        List<Item> items = List.of(item);
        List<Booking> bookings = List.of(booking);
        when(itemRepository.findAllByOwnerId(owner.getId())).thenReturn(items);
        when(bookingRepository.findByOwnerIdAndStatus(eq(owner.getId()), eq(BookingStatus.REJECTED), any(Pageable.class)))
                .thenReturn(bookings);

        // when
        List<BookingResponseDto> results = bookingService.getOwnerBookings(owner.getId(), BookingState.REJECTED, 0, 10);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(bookingRepository).findByOwnerIdAndStatus(eq(owner.getId()), eq(BookingStatus.REJECTED), any(Pageable.class));
    }

    @Test
    void getOwnerBookings_UserHasNoItems_ShouldThrowException() {
        // given
        when(itemRepository.findAllByOwnerId(owner.getId())).thenReturn(List.of());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.getOwnerBookings(owner.getId(), BookingState.ALL, 0, 10));

        assertEquals("User has no items", exception.getMessage());
        verify(bookingRepository, never()).findAllByItemOwnerIdOrderByStartDesc(anyLong(), any());
    }

    @Test
    void getOwnerBookings_WithInvalidState_ShouldThrowException() {
        // given
        List<Item> items = List.of(item);
        when(itemRepository.findAllByOwnerId(owner.getId())).thenReturn(items);
        BookingState invalidState = null;

        // when & then
        assertThrows(NullPointerException.class,
                () -> bookingService.getOwnerBookings(owner.getId(), invalidState, 0, 10));
    }
}