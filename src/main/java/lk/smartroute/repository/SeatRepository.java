package lk.smartroute.repository;

import lk.smartroute.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByBusIdOrderBySeatNumber(Long busId);

    Optional<Seat> findByBusIdAndSeatNumber(Long busId, int seatNumber);

    @Query("""
        select s
        from Seat s
        join Schedule sc on sc.bus = s.bus
        where sc.id = :scheduleId
          and s.id not in (
              select bs.seat.id
              from BookingSeat bs
              join bs.booking b
              where bs.schedule.id = :scheduleId and (b.bookingStatus = lk.smartroute.model.BookingStatus.CONFIRMED or (b.bookingStatus = lk.smartroute.model.BookingStatus.PENDING_PAYMENT and (b.paymentDueAt is null or b.paymentDueAt > CURRENT_TIMESTAMP)))
          )
        order by s.seatNumber
        """)
    List<Seat> findAvailableSeatsForSchedule(@Param("scheduleId") Long scheduleId);

    @Query("""
        select s
        from BookingSeat bs
        join bs.seat s
        join bs.booking b
        where bs.schedule.id = :scheduleId and (b.bookingStatus = lk.smartroute.model.BookingStatus.CONFIRMED or (b.bookingStatus = lk.smartroute.model.BookingStatus.PENDING_PAYMENT and (b.paymentDueAt is null or b.paymentDueAt > CURRENT_TIMESTAMP)))
        order by s.seatNumber
        """)
    List<Seat> findReservedSeatsForSchedule(@Param("scheduleId") Long scheduleId);

    @Query("""
        select count(s)
        from Seat s
        join Schedule sc on sc.bus = s.bus
        where sc.id = :scheduleId
          and s.id not in (
              select bs.seat.id
              from BookingSeat bs
              join bs.booking b
              where bs.schedule.id = :scheduleId and (b.bookingStatus = lk.smartroute.model.BookingStatus.CONFIRMED or (b.bookingStatus = lk.smartroute.model.BookingStatus.PENDING_PAYMENT and (b.paymentDueAt is null or b.paymentDueAt > CURRENT_TIMESTAMP)))
          )
        """)
    long countAvailableSeatsForSchedule(@Param("scheduleId") Long scheduleId);
}
