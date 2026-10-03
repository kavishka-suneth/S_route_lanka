package lk.smartroute.repository;

import lk.smartroute.model.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Set;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    @Query("select bs.seat.id from BookingSeat bs join bs.booking b where bs.schedule.id = :scheduleId and (b.bookingStatus = lk.smartroute.model.BookingStatus.CONFIRMED or (b.bookingStatus = lk.smartroute.model.BookingStatus.PENDING_PAYMENT and (b.paymentDueAt is null or b.paymentDueAt > CURRENT_TIMESTAMP)))")
    Set<Long> findReservedSeatIdsByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("select bs.seat.id from BookingSeat bs join bs.booking b where bs.schedule.id = :scheduleId and (b.bookingStatus = lk.smartroute.model.BookingStatus.CONFIRMED or (b.bookingStatus = lk.smartroute.model.BookingStatus.PENDING_PAYMENT and (b.paymentDueAt is null or b.paymentDueAt > CURRENT_TIMESTAMP))) and bs.booking.id <> :bookingId")
    Set<Long> findReservedSeatIdsByScheduleIdExcludingBooking(@Param("scheduleId") Long scheduleId, @Param("bookingId") Long bookingId);

    long countByScheduleId(Long scheduleId);

    List<BookingSeat> findByScheduleId(Long scheduleId);

    @Query("""
        select coalesce(max(bs.seat.seatNumber), 0)
        from BookingSeat bs
        join bs.booking b
        where bs.schedule.id = :scheduleId and (b.bookingStatus = lk.smartroute.model.BookingStatus.CONFIRMED or (b.bookingStatus = lk.smartroute.model.BookingStatus.PENDING_PAYMENT and (b.paymentDueAt is null or b.paymentDueAt > CURRENT_TIMESTAMP)))
          and b.bookingStatus <> lk.smartroute.model.BookingStatus.CANCELLED
        """)
    int maxReservedSeatNumberForSchedule(@Param("scheduleId") Long scheduleId);
}
