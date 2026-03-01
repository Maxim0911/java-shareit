package ru.practicum.shareit.RepositoryTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BookingRepository Integration Tests")
class BookingRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private BookingRepository bookingRepository;

    private User owner;
    private User booker;
    private User anotherBooker;
    private Item item1;
    private Item item2;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@example.com");
        em.persist(owner);

        booker = new User();
        booker.setName("Booker");
        booker.setEmail("booker@example.com");
        em.persist(booker);

        anotherBooker = new User();
        anotherBooker.setName("Another Booker");
        anotherBooker.setEmail("another.booker@example.com");
        em.persist(anotherBooker);

        item1 = Item.builder()
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .owner(owner)
                .build();
        em.persist(item1);

        item2 = Item.builder()
                .name("Hammer")
                .description("Heavy hammer")
                .available(true)
                .owner(owner)
                .build();
        em.persist(item2);
    }

    private Booking createBooking(LocalDateTime start, LocalDateTime end, BookingStatus status, Item item, User booker) {
        Booking booking = new Booking();
        booking.setStart(start);
        booking.setEnd(end);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(status);
        return booking;
    }

    @Test
    @DisplayName("Should save booking")
    void shouldSaveBooking() {
        // Given
        Booking booking = createBooking(now.plusDays(1), now.plusDays(2), BookingStatus.WAITING, item1, booker);

        // When
        Booking savedBooking = bookingRepository.save(booking);

        // Then
        assertThat(savedBooking.getId()).isNotNull();
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(savedBooking.getBooker().getId()).isEqualTo(booker.getId());
        assertThat(savedBooking.getItem().getId()).isEqualTo(item1.getId());
    }

    @Test
    @DisplayName("Should find bookings by booker id")
    void shouldFindBookingsByBookerId() {
        // Given
        Booking booking1 = createBooking(now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED, item1, booker);
        Booking booking2 = createBooking(now.plusDays(3), now.plusDays(4), BookingStatus.WAITING, item2, booker);
        em.persist(booking1);
        em.persist(booking2);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Booking> bookings = bookingRepository.findAllByBookerIdOrderByStartDesc(booker.getId(), pageable);

        // Then
        assertThat(bookings).hasSize(2);
        assertThat(bookings.get(0).getStart()).isAfter(bookings.get(1).getStart());
    }

    @Test
    @DisplayName("Should find current bookings for booker")
    void shouldFindCurrentBookingsForBooker() {
        // Given
        Booking currentBooking = createBooking(now.minusHours(1), now.plusHours(1), BookingStatus.APPROVED, item1, booker);
        Booking pastBooking = createBooking(now.minusDays(2), now.minusDays(1), BookingStatus.APPROVED, item2, booker);
        Booking futureBooking = createBooking(now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED, item1, anotherBooker);

        em.persist(currentBooking);
        em.persist(pastBooking);
        em.persist(futureBooking);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Booking> currentBookings = bookingRepository.findCurrentByBookerId(
                booker.getId(), now, pageable);

        // Then
        assertThat(currentBookings).hasSize(1);
        assertThat(currentBookings.get(0).getId()).isEqualTo(currentBooking.getId());
    }

    @Test
    @DisplayName("Should find past bookings for booker")
    void shouldFindPastBookingsForBooker() {
        // Given
        Booking pastBooking1 = createBooking(now.minusDays(3), now.minusDays(2), BookingStatus.APPROVED, item1, booker);
        Booking pastBooking2 = createBooking(now.minusDays(1), now.minusHours(1), BookingStatus.APPROVED, item2, booker);
        Booking futureBooking = createBooking(now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED, item1, anotherBooker);

        em.persist(pastBooking1);
        em.persist(pastBooking2);
        em.persist(futureBooking);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Booking> pastBookings = bookingRepository.findAllByBookerIdAndEndBeforeOrderByStartDesc(
                booker.getId(), now, pageable);

        // Then
        assertThat(pastBookings).hasSize(2);
        assertThat(pastBookings.get(0).getEnd()).isBefore(now);
    }

    @Test
    @DisplayName("Should find future bookings for booker")
    void shouldFindFutureBookingsForBooker() {
        // Given
        Booking futureBooking1 = createBooking(now.plusHours(1), now.plusDays(1), BookingStatus.APPROVED, item1, booker);
        Booking futureBooking2 = createBooking(now.plusDays(2), now.plusDays(3), BookingStatus.APPROVED, item2, booker);
        Booking pastBooking = createBooking(now.minusDays(1), now.minusHours(1), BookingStatus.APPROVED, item1, anotherBooker);

        em.persist(futureBooking1);
        em.persist(futureBooking2);
        em.persist(pastBooking);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Booking> futureBookings = bookingRepository.findAllByBookerIdAndStartAfterOrderByStartDesc(
                booker.getId(), now, pageable);

        // Then
        assertThat(futureBookings).hasSize(2);
        assertThat(futureBookings.get(0).getStart()).isAfter(now);
    }

    @Test
    @DisplayName("Should find bookings by booker id and status")
    void shouldFindBookingsByBookerIdAndStatus() {
        // Given
        Booking waitingBooking = createBooking(now.plusDays(1), now.plusDays(2), BookingStatus.WAITING, item1, booker);
        Booking approvedBooking = createBooking(now.plusDays(3), now.plusDays(4), BookingStatus.APPROVED, item2, booker);
        Booking rejectedBooking = createBooking(now.plusDays(5), now.plusDays(6), BookingStatus.REJECTED, item1, anotherBooker);

        em.persist(waitingBooking);
        em.persist(approvedBooking);
        em.persist(rejectedBooking);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Booking> waitingBookings = bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(
                booker.getId(), BookingStatus.WAITING, pageable);

        // Then
        assertThat(waitingBookings).hasSize(1);
        assertThat(waitingBookings.get(0).getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    @DisplayName("Should find bookings by owner id")
    void shouldFindBookingsByOwnerId() {
        // Given
        Booking booking1 = createBooking(now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED, item1, booker);
        Booking booking2 = createBooking(now.plusDays(3), now.plusDays(4), BookingStatus.WAITING, item2, anotherBooker);
        em.persist(booking1);
        em.persist(booking2);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Booking> bookings = bookingRepository.findAllByItemOwnerIdOrderByStartDesc(owner.getId(), pageable);

        // Then
        assertThat(bookings).hasSize(2);
        assertThat(bookings.get(0).getItem().getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("Should find overlapping bookings")
    void shouldFindOverlappingBookings() {
        // Given
        Booking approvedBooking = createBooking(now.plusDays(1), now.plusDays(3), BookingStatus.APPROVED, item1, booker);
        em.persist(approvedBooking);

        // When - overlapping booking
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                item1.getId(),
                now.plusDays(2), // start during existing booking
                now.plusDays(4)  // end after existing booking
        );

        // Then
        assertThat(overlapping).hasSize(1);
        assertThat(overlapping.get(0).getId()).isEqualTo(approvedBooking.getId());

        // When - non-overlapping booking
        List<Booking> nonOverlapping = bookingRepository.findOverlappingBookings(
                item1.getId(),
                now.plusDays(4),  // start after existing booking ends
                now.plusDays(5)
        );

        // Then
        assertThat(nonOverlapping).isEmpty();
    }

    @Test
    @DisplayName("Should find booking by booker, item and status for comment")
    void shouldFindBookingForComment() {
        // Given
        Booking pastBooking = createBooking(now.minusDays(2), now.minusDays(1), BookingStatus.APPROVED, item1, booker);
        em.persist(pastBooking);

        // When
        Optional<Booking> foundBooking = bookingRepository.findFirstByBookerIdAndItemIdAndStatusAndEndBefore(
                booker.getId(), item1.getId(), BookingStatus.APPROVED, now);

        // Then
        assertThat(foundBooking).isPresent();
        assertThat(foundBooking.get().getId()).isEqualTo(pastBooking.getId());

        // When - future booking
        Optional<Booking> futureBooking = bookingRepository.findFirstByBookerIdAndItemIdAndStatusAndEndBefore(
                booker.getId(), item1.getId(), BookingStatus.APPROVED, now.minusDays(3));

        // Then
        assertThat(futureBooking).isEmpty();
    }

    @Test
    @DisplayName("Should find last booking for item - exactly one result")
    void shouldFindLastBookingForItem() {
        // Given - создаем только одно прошедшее бронирование для item1
        Booking lastBooking = createBooking(now.minusDays(2), now.minusDays(1), BookingStatus.APPROVED, item1, booker);
        em.persist(lastBooking);

        // Создаем бронирования для других предметов, чтобы они не мешали
        Booking otherItemBooking = createBooking(now.minusDays(4), now.minusDays(3), BookingStatus.APPROVED, item2, anotherBooker);
        em.persist(otherItemBooking);

        // When
        Optional<Booking> found = bookingRepository.findLastBookingForItem(item1.getId(), now);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(lastBooking.getId());
        assertThat(found.get().getEnd()).isBefore(now);
    }

    @Test
    @DisplayName("Should return empty optional when no last booking exists")
    void shouldReturnEmptyWhenNoLastBooking() {
        // Given - нет прошедших бронирований для item1
        Booking futureBooking = createBooking(now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED, item1, booker);
        em.persist(futureBooking);

        // When
        Optional<Booking> found = bookingRepository.findLastBookingForItem(item1.getId(), now);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find next booking for item - exactly one result")
    void shouldFindNextBookingForItem() {
        // Given - создаем только одно будущее бронирование для item1
        Booking nextBooking = createBooking(now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED, item1, booker);
        em.persist(nextBooking);

        // Создаем бронирования для других предметов, чтобы они не мешали
        Booking otherItemBooking = createBooking(now.plusDays(3), now.plusDays(4), BookingStatus.APPROVED, item2, anotherBooker);
        em.persist(otherItemBooking);

        // When
        Optional<Booking> found = bookingRepository.findNextBookingForItem(item1.getId(), now);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(nextBooking.getId());
        assertThat(found.get().getStart()).isAfter(now);
    }

    @Test
    @DisplayName("Should return empty optional when no next booking exists")
    void shouldReturnEmptyWhenNoNextBooking() {
        // Given - нет будущих бронирований для item1
        Booking pastBooking = createBooking(now.minusDays(2), now.minusDays(1), BookingStatus.APPROVED, item1, booker);
        em.persist(pastBooking);

        // When
        Optional<Booking> found = bookingRepository.findNextBookingForItem(item1.getId(), now);

        // Then
        assertThat(found).isEmpty();
    }


    @Test
    @DisplayName("Should find next bookings for multiple items")
    void shouldFindNextBookingsForItems() {
        // Given
        Booking nextBooking1 = createBooking(now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED, item1, booker);
        Booking nextBooking2 = createBooking(now.plusDays(2), now.plusDays(3), BookingStatus.APPROVED, item2, anotherBooker);
        Booking laterBooking1 = createBooking(now.plusDays(5), now.plusDays(6), BookingStatus.APPROVED, item1, anotherBooker);

        em.persist(nextBooking1);
        em.persist(nextBooking2);
        em.persist(laterBooking1);

        // When
        List<Booking> nextBookings = bookingRepository.findNextBookingsForItems(
                List.of(item1.getId(), item2.getId()), now);

        // Then
        assertThat(nextBookings).hasSize(2);
        assertThat(nextBookings).extracting(Booking::getId)
                .containsExactlyInAnyOrder(nextBooking1.getId(), nextBooking2.getId());
    }
}