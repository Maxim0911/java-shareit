package ru.practicum.shareit.booking.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByBookerIdOrderByStartDesc(Long bookerId, Pageable pageable);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.booker.id = :bookerId " +
            "AND b.start <= :currentTime " +
            "AND b.end >= :currentTime " +
            "ORDER BY b.start DESC")
    List<Booking> findCurrentByBookerId(@Param("bookerId") Long bookerId,
                                        @Param("currentTime") LocalDateTime currentTime,
                                        Pageable pageable);

    List<Booking> findAllByBookerIdAndEndBeforeOrderByStartDesc(Long bookerId,
                                                                LocalDateTime currentTime,
                                                                Pageable pageable);

    List<Booking> findAllByBookerIdAndStartAfterOrderByStartDesc(Long bookerId,
                                                                 LocalDateTime currentTime,
                                                                 Pageable pageable);

    List<Booking> findAllByBookerIdAndStatusOrderByStartDesc(Long bookerId,
                                                             BookingStatus status,
                                                             Pageable pageable);

    List<Booking> findAllByItemOwnerIdOrderByStartDesc(Long ownerId, Pageable pageable);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.start <= :currentTime " +
            "AND b.end >= :currentTime " +
            "ORDER BY b.start DESC")
    List<Booking> findCurrentByOwnerId(@Param("ownerId") Long ownerId,
                                       @Param("currentTime") LocalDateTime currentTime,
                                       Pageable pageable);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.end < :currentTime " +
            "ORDER BY b.start DESC")
    List<Booking> findPastByOwnerId(@Param("ownerId") Long ownerId,
                                    @Param("currentTime") LocalDateTime currentTime,
                                    Pageable pageable);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.start > :currentTime " +
            "ORDER BY b.start DESC")
    List<Booking> findFutureByOwnerId(@Param("ownerId") Long ownerId,
                                      @Param("currentTime") LocalDateTime currentTime,
                                      Pageable pageable);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.status = :status " +
            "ORDER BY b.start DESC")
    List<Booking> findByOwnerIdAndStatus(@Param("ownerId") Long ownerId,
                                         @Param("status") BookingStatus status,
                                         Pageable pageable);


    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.status = 'APPROVED' " +
            "AND b.start < :end " +
            "AND b.end > :start")
    List<Booking> findOverlappingBookings(@Param("itemId") Long itemId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    Optional<Booking> findFirstByBookerIdAndItemIdAndStatusAndEndBefore(Long bookerId,
                                                                        Long itemId,
                                                                        BookingStatus status,
                                                                        LocalDateTime currentTime);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id IN :itemIds " +
            "AND b.status = 'APPROVED' " +
            "AND b.end < :now " +
            "AND b.id IN (" +
            "   SELECT MAX(b2.id) FROM Booking b2 " +
            "   WHERE b2.item.id = b.item.id " +
            "   AND b2.status = 'APPROVED' " +
            "   AND b2.end < :now " +
            "   GROUP BY b2.item.id" +
            ")")
    List<Booking> findLastBookingsForItems(@Param("itemIds") List<Long> itemIds,
                                           @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id IN :itemIds " +
            "AND b.status = 'APPROVED' " +
            "AND b.start > :now " +
            "AND b.id IN (" +
            "   SELECT MIN(b2.id) FROM Booking b2 " +
            "   WHERE b2.item.id = b.item.id " +
            "   AND b2.status = 'APPROVED' " +
            "   AND b2.start > :now " +
            "   GROUP BY b2.item.id" +
            ")")
    List<Booking> findNextBookingsForItems(@Param("itemIds") List<Long> itemIds,
                                           @Param("now") LocalDateTime now);


    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.status = 'APPROVED' " +
            "AND b.end < :now " +
            "ORDER BY b.end DESC")
    Optional<Booking> findLastBookingForItem(@Param("itemId") Long itemId,
                                             @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.status = 'APPROVED' " +
            "AND b.start > :now " +
            "ORDER BY b.start ASC")
    Optional<Booking> findNextBookingForItem(@Param("itemId") Long itemId,
                                             @Param("now") LocalDateTime now);
}