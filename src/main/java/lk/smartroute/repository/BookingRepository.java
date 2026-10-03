package lk.smartroute.repository;

import lk.smartroute.model.Booking;
import lk.smartroute.model.BookingStatus;
import org.springframework.data.domain.Page;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    boolean existsByBookingReference(String bookingReference);

    Page<Booking> findByPassengerIdOrderByBookingDateDesc(Long passengerId, Pageable pageable);

    List<Booking> findByScheduleId(Long scheduleId);

    List<Booking> findByScheduleIdAndBookingStatusIn(Long scheduleId, List<BookingStatus> bookingStatuses);

    List<Booking> findByBookingStatus(BookingStatus bookingStatus);
    List<Booking> findByBookingStatusAndPaymentDueAtBefore(BookingStatus bookingStatus, LocalDateTime time);

    List<Booking> findByBookingStatusAndPaymentDueAtBetweenAndPaymentDeadlineWarningSentAtIsNull(BookingStatus bookingStatus, LocalDateTime from, LocalDateTime to);

    List<Booking> findByBookingStatusAndPaymentDueAtBeforeAndExpiredNotificationSentAtIsNull(BookingStatus bookingStatus, LocalDateTime time);

    List<Booking> findByBookingDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("""
        select b
        from Booking b
        join fetch b.schedule s
        join fetch s.route
        join fetch s.bus
        join fetch b.passenger p
        join fetch p.user
        where b.bookingStatus = lk.smartroute.model.BookingStatus.CONFIRMED
          and s.status in (lk.smartroute.model.ScheduleStatus.SCHEDULED, lk.smartroute.model.ScheduleStatus.DELAYED)
          and s.travelDate = :date
          and s.departureTime between :from and :to
          and (
            (:kind = '24H' and b.reminder24hSentAt is null)
            or (:kind = '2H' and b.reminder2hSentAt is null)
            or (:kind = '30M' and b.reminder30mSentAt is null)
          )
        """)
    List<Booking> findConfirmedReminderCandidates(
        @Param("date") LocalDate date,
        @Param("from") LocalTime from,
        @Param("to") LocalTime to,
        @Param("kind") String kind
    );

    @Query("""
        select b
        from Booking b
        join b.passenger passenger
        join passenger.user u
        join b.schedule s
        join s.route r
        where (:status is null or b.bookingStatus = :status)
          and (
            :keyword is null
            or :keyword = ''
            or lower(b.bookingReference) like lower(concat('%', :keyword, '%'))
            or lower(u.firstName) like lower(concat('%', :keyword, '%'))
            or lower(u.lastName) like lower(concat('%', :keyword, '%'))
            or lower(u.email) like lower(concat('%', :keyword, '%'))
            or lower(r.origin) like lower(concat('%', :keyword, '%'))
            or lower(r.destination) like lower(concat('%', :keyword, '%'))
          )
        """)
    Page<Booking> searchBookings(
        @Param("status") BookingStatus status,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("""
        select b
        from Booking b
        where b.passenger.id = :passengerId
          and b.bookingStatus = lk.smartroute.model.BookingStatus.CONFIRMED
          and (b.schedule.travelDate > :today
               or (b.schedule.travelDate = :today and b.schedule.departureTime >= :time))
        order by b.schedule.travelDate, b.schedule.departureTime
        """)
    List<Booking> findUpcomingConfirmedBookingsForPassenger(
        @Param("passengerId") Long passengerId,
        @Param("today") LocalDate today,
        @Param("time") LocalTime time
    );

    long countByScheduleId(Long scheduleId);

    @Query("select coalesce(sum(b.passengerCount), 0) from Booking b where b.schedule.id = :scheduleId and b.bookingStatus <> lk.smartroute.model.BookingStatus.CANCELLED")
    long totalPassengersBookedForSchedule(@Param("scheduleId") Long scheduleId);

    boolean existsByIdAndPassengerId(Long bookingId, Long passengerId);

    boolean existsByPassengerId(Long passengerId);

    List<Booking> findByBookingStatusOrderByBookingDateDesc(BookingStatus bookingStatus);

    long countByBookingStatus(BookingStatus bookingStatus);

    @Query("""
        select count(b)
        from Booking b
        where (:status is null or b.bookingStatus = :status)
          and (:from is null or b.bookingDate >= :from)
          and (:to is null or b.bookingDate <= :to)
        """)
    long countByStatusAndBookingDateBetween(
        @Param("status") BookingStatus status,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
