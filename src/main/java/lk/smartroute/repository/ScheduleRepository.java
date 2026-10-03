package lk.smartroute.repository;

import jakarta.persistence.LockModeType;
import lk.smartroute.model.Schedule;
import lk.smartroute.model.ScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("""
        select sc
        from Schedule sc
        join sc.route r
        where lower(r.origin) = lower(:origin)
          and lower(r.destination) = lower(:destination)
          and r.active = true
          and sc.bus.status = lk.smartroute.model.BusStatus.ACTIVE
          and sc.travelDate = :travelDate
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
        order by sc.departureTime
        """)
    Page<Schedule> searchScheduledTrips(@Param("origin") String origin, @Param("destination") String destination, @Param("travelDate") LocalDate travelDate, Pageable pageable);

    @Query("""
        select sc
        from Schedule sc
        where (sc.travelDate > :today
          or (sc.travelDate = :today and sc.departureTime >= :time))
          and sc.route.active = true
          and sc.bus.status = lk.smartroute.model.BusStatus.ACTIVE
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
        order by sc.travelDate, sc.departureTime
        """)
    Page<Schedule> findUpcomingSchedules(@Param("today") LocalDate today, @Param("time") LocalTime time, Pageable pageable);

    @Query("""
        select sc
        from Schedule sc
        where (sc.travelDate > :today
          or (sc.travelDate = :today and sc.departureTime >= :time))
          and sc.route.active = true
          and sc.bus.status = lk.smartroute.model.BusStatus.ACTIVE
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
        order by sc.travelDate, sc.departureTime
        """)
    List<Schedule> findUpcomingSchedules(@Param("today") LocalDate today, @Param("time") LocalTime time);

    List<Schedule> findByStatus(ScheduleStatus status);

    List<Schedule> findByBusId(Long busId);

    List<Schedule> findByDriverId(Long driverId);

    List<Schedule> findByRouteId(Long routeId);

    List<Schedule> findByTravelDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("""
        select sc
        from Schedule sc
        join fetch sc.route
        where sc.bus.id = :busId
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
          and (sc.travelDate > :today or (sc.travelDate = :today and sc.arrivalTime > :time))
        order by sc.travelDate, sc.departureTime
        """)
    List<Schedule> findCurrentOrNextActiveBusAssignment(
        @Param("busId") Long busId,
        @Param("today") LocalDate today,
        @Param("time") LocalTime time,
        Pageable pageable
    );

    @Query("""
        select sc
        from Schedule sc
        join fetch sc.route
        where sc.driver.id = :driverId
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
          and (sc.travelDate > :today or (sc.travelDate = :today and sc.arrivalTime > :time))
        order by sc.travelDate, sc.departureTime
        """)
    List<Schedule> findCurrentOrNextActiveDriverAssignment(
        @Param("driverId") Long driverId,
        @Param("today") LocalDate today,
        @Param("time") LocalTime time,
        Pageable pageable
    );

    @Query("""
        select count(sc)
        from Schedule sc
        where sc.bus.id = :busId
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
          and sc.travelDate = :travelDate
          and :newDeparture < sc.arrivalTime
          and :newArrival > sc.departureTime
        """)
    long countOverlappingBusAssignments(
        @Param("busId") Long busId,
        @Param("travelDate") LocalDate travelDate,
        @Param("newDeparture") LocalTime newDeparture,
        @Param("newArrival") LocalTime newArrival
    );

    @Query("""
        select count(sc)
        from Schedule sc
        where (:scheduleId is null or sc.id <> :scheduleId)
          and sc.bus.id = :busId
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
          and sc.travelDate = :travelDate
          and :newDeparture < sc.arrivalTime
          and :newArrival > sc.departureTime
        """)
    long countOverlappingBusAssignmentsExcluding(
        @Param("scheduleId") Long scheduleId,
        @Param("busId") Long busId,
        @Param("travelDate") LocalDate travelDate,
        @Param("newDeparture") LocalTime newDeparture,
        @Param("newArrival") LocalTime newArrival
    );

    @Query("""
        select count(sc)
        from Schedule sc
        where sc.driver.id = :driverId
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
          and sc.travelDate = :travelDate
          and :newDeparture < sc.arrivalTime
          and :newArrival > sc.departureTime
        """)
    long countOverlappingDriverAssignments(
        @Param("driverId") Long driverId,
        @Param("travelDate") LocalDate travelDate,
        @Param("newDeparture") LocalTime newDeparture,
        @Param("newArrival") LocalTime newArrival
    );

    @Query("""
        select count(sc)
        from Schedule sc
        where (:scheduleId is null or sc.id <> :scheduleId)
          and sc.driver.id = :driverId
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
          and sc.travelDate = :travelDate
          and :newDeparture < sc.arrivalTime
          and :newArrival > sc.departureTime
        """)
    long countOverlappingDriverAssignmentsExcluding(
        @Param("scheduleId") Long scheduleId,
        @Param("driverId") Long driverId,
        @Param("travelDate") LocalDate travelDate,
        @Param("newDeparture") LocalTime newDeparture,
        @Param("newArrival") LocalTime newArrival
    );

    @Query("select count(b) from Booking b where b.schedule.id = :scheduleId")
    long countBookingsForSchedule(@Param("scheduleId") Long scheduleId);

    @Query("""
        select coalesce(sum(b.passengerCount), 0)
        from Booking b
        where b.schedule.id = :scheduleId
          and b.bookingStatus <> lk.smartroute.model.BookingStatus.CANCELLED
        """)
    long totalPassengersForSchedule(@Param("scheduleId") Long scheduleId);

    @Query("""
        select (count(bs.id) * 100.0) / sc.bus.capacity
        from Schedule sc
        left join BookingSeat bs on bs.schedule = sc
        left join bs.booking b
        where sc.id = :scheduleId
          and (b is null or b.bookingStatus in (lk.smartroute.model.BookingStatus.PENDING_PAYMENT, lk.smartroute.model.BookingStatus.CONFIRMED, lk.smartroute.model.BookingStatus.COMPLETED))
        group by sc.id, sc.bus.capacity
        """)
    Double occupancyPercentage(@Param("scheduleId") Long scheduleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sc from Schedule sc join fetch sc.bus join fetch sc.route join fetch sc.driver where sc.id = :scheduleId")
    Optional<Schedule> findByIdForUpdate(@Param("scheduleId") Long scheduleId);

    @Query(
        value = """
            select sc.id as scheduleId,
                   r.id as routeId,
                   concat(r.origin, ' to ', r.destination) as route,
                   sc.travelDate as travelDate,
                   sc.departureTime as departureTime,
                   bus.registrationNo as busRegistrationNo,
                   bus.capacity as busCapacity,
                   coalesce(sum(case
                       when b.bookingStatus in (lk.smartroute.model.BookingStatus.CONFIRMED, lk.smartroute.model.BookingStatus.COMPLETED)
                        and exists (
                            select p.id from Payment p
                            where p.booking = b
                              and p.paymentStatus = lk.smartroute.model.PaymentStatus.SUCCESS
                        )
                       then b.passengerCount else 0 end), 0) as ticketsSold,
                   coalesce(sum(case
                       when b.bookingStatus in (lk.smartroute.model.BookingStatus.CONFIRMED, lk.smartroute.model.BookingStatus.COMPLETED)
                        and exists (
                            select p.id from Payment p
                            where p.booking = b
                              and p.paymentStatus = lk.smartroute.model.PaymentStatus.SUCCESS
                        )
                       then 1 else 0 end), 0) as bookingCount,
                   coalesce(sum(case
                       when b.bookingStatus in (lk.smartroute.model.BookingStatus.CONFIRMED, lk.smartroute.model.BookingStatus.COMPLETED)
                        and exists (
                            select p.id from Payment p
                            where p.booking = b
                              and p.paymentStatus = lk.smartroute.model.PaymentStatus.SUCCESS
                        )
                       then b.totalAmount else 0 end), 0) as ticketSalesAmount
            from Schedule sc
            join sc.route r
            join sc.bus bus
            left join Booking b on b.schedule = sc
            where (:from is null or sc.travelDate >= :from)
              and (:to is null or sc.travelDate <= :to)
              and (:routeId is null or r.id = :routeId)
              and (:scheduleId is null or sc.id = :scheduleId)
            group by sc.id, r.id, r.origin, r.destination, sc.travelDate, sc.departureTime, bus.registrationNo, bus.capacity
            order by sc.travelDate desc, sc.departureTime desc, sc.id
            """,
        countQuery = """
            select count(sc)
            from Schedule sc
            where (:from is null or sc.travelDate >= :from)
              and (:to is null or sc.travelDate <= :to)
              and (:routeId is null or sc.route.id = :routeId)
              and (:scheduleId is null or sc.id = :scheduleId)
            """
    )
    Page<TicketSalesScheduleProjection> ticketSalesDetails(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("routeId") Long routeId,
        @Param("scheduleId") Long scheduleId,
        Pageable pageable
    );

    @Query("""
        select sc.id as scheduleId,
               r.id as routeId,
               concat(r.origin, ' to ', r.destination) as route,
               sc.travelDate as travelDate,
               sc.departureTime as departureTime,
               bus.registrationNo as busRegistrationNo,
               bus.capacity as busCapacity,
               coalesce(sum(case
                   when b.bookingStatus in (lk.smartroute.model.BookingStatus.CONFIRMED, lk.smartroute.model.BookingStatus.COMPLETED)
                    and exists (
                        select p.id from Payment p
                        where p.booking = b
                          and p.paymentStatus = lk.smartroute.model.PaymentStatus.SUCCESS
                    )
                   then b.passengerCount else 0 end), 0) as ticketsSold,
               coalesce(sum(case
                   when b.bookingStatus in (lk.smartroute.model.BookingStatus.CONFIRMED, lk.smartroute.model.BookingStatus.COMPLETED)
                    and exists (
                        select p.id from Payment p
                        where p.booking = b
                          and p.paymentStatus = lk.smartroute.model.PaymentStatus.SUCCESS
                    )
                   then 1 else 0 end), 0) as bookingCount,
               coalesce(sum(case
                   when b.bookingStatus in (lk.smartroute.model.BookingStatus.CONFIRMED, lk.smartroute.model.BookingStatus.COMPLETED)
                    and exists (
                        select p.id from Payment p
                        where p.booking = b
                          and p.paymentStatus = lk.smartroute.model.PaymentStatus.SUCCESS
                    )
                   then b.totalAmount else 0 end), 0) as ticketSalesAmount
        from Schedule sc
        join sc.route r
        join sc.bus bus
        left join Booking b on b.schedule = sc
        where (:from is null or sc.travelDate >= :from)
          and (:to is null or sc.travelDate <= :to)
          and (:routeId is null or r.id = :routeId)
          and (:scheduleId is null or sc.id = :scheduleId)
        group by sc.id, r.id, r.origin, r.destination, sc.travelDate, sc.departureTime, bus.registrationNo, bus.capacity
        order by sc.travelDate desc, sc.departureTime desc, sc.id
        """)
    List<TicketSalesScheduleProjection> ticketSalesDetailsList(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("routeId") Long routeId,
        @Param("scheduleId") Long scheduleId
    );

    @Query(
        value = """
            select sc.id as scheduleId,
                   concat(r.origin, ' to ', r.destination) as route,
                   sc.travelDate as travelDate,
                   sc.departureTime as departureTime,
                   bus.registrationNo as busRegistrationNo,
                   bus.capacity as busCapacity,
                   coalesce(sum(case
                       when b is not null and b.bookingStatus <> lk.smartroute.model.BookingStatus.CANCELLED
                       then 1 else 0 end), 0) as occupiedSeats
            from Schedule sc
            join sc.route r
            join sc.bus bus
            left join BookingSeat bs on bs.schedule = sc
            left join bs.booking b
            where (:from is null or sc.travelDate >= :from)
              and (:to is null or sc.travelDate <= :to)
              and (:route is null
                   or lower(r.origin) like lower(concat('%', :route, '%'))
                   or lower(r.destination) like lower(concat('%', :route, '%'))
                   or lower(concat(r.origin, ' to ', r.destination)) like lower(concat('%', :route, '%')))
              and (:travelDate is null or sc.travelDate = :travelDate)
              and (:routeId is null or r.id = :routeId)
              and (:scheduleId is null or sc.id = :scheduleId)
            group by sc.id, r.origin, r.destination, sc.travelDate, sc.departureTime, bus.registrationNo, bus.capacity
            order by sc.travelDate desc, sc.departureTime desc, sc.id
            """,
        countQuery = """
            select count(sc)
            from Schedule sc
            join sc.route r
            where (:from is null or sc.travelDate >= :from)
              and (:to is null or sc.travelDate <= :to)
              and (:route is null
                   or lower(r.origin) like lower(concat('%', :route, '%'))
                   or lower(r.destination) like lower(concat('%', :route, '%'))
                   or lower(concat(r.origin, ' to ', r.destination)) like lower(concat('%', :route, '%')))
              and (:travelDate is null or sc.travelDate = :travelDate)
              and (:routeId is null or r.id = :routeId)
              and (:scheduleId is null or sc.id = :scheduleId)
            """
    )
    Page<ScheduleOccupancyProjection> occupancyDetails(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("route") String route,
        @Param("travelDate") LocalDate travelDate,
        @Param("routeId") Long routeId,
        @Param("scheduleId") Long scheduleId,
        Pageable pageable
    );

    @Query("""
        select sc.id as scheduleId,
               concat(r.origin, ' to ', r.destination) as route,
               sc.travelDate as travelDate,
               sc.departureTime as departureTime,
               bus.registrationNo as busRegistrationNo,
               bus.capacity as busCapacity,
               coalesce(sum(case
                   when b is not null and b.bookingStatus <> lk.smartroute.model.BookingStatus.CANCELLED
                   then 1 else 0 end), 0) as occupiedSeats
        from Schedule sc
        join sc.route r
        join sc.bus bus
        left join BookingSeat bs on bs.schedule = sc
        left join bs.booking b
        where (:from is null or sc.travelDate >= :from)
          and (:to is null or sc.travelDate <= :to)
          and (:route is null
               or lower(r.origin) like lower(concat('%', :route, '%'))
               or lower(r.destination) like lower(concat('%', :route, '%'))
               or lower(concat(r.origin, ' to ', r.destination)) like lower(concat('%', :route, '%')))
          and (:travelDate is null or sc.travelDate = :travelDate)
          and (:routeId is null or r.id = :routeId)
          and (:scheduleId is null or sc.id = :scheduleId)
        group by sc.id, r.origin, r.destination, sc.travelDate, sc.departureTime, bus.registrationNo, bus.capacity
        order by sc.travelDate desc, sc.departureTime desc, sc.id
        """)
    List<ScheduleOccupancyProjection> occupancyDetailsList(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("route") String route,
        @Param("travelDate") LocalDate travelDate,
        @Param("routeId") Long routeId,
        @Param("scheduleId") Long scheduleId
    );

    interface TicketSalesScheduleProjection {
        Long getScheduleId();
        Long getRouteId();
        String getRoute();
        LocalDate getTravelDate();
        LocalTime getDepartureTime();
        String getBusRegistrationNo();
        int getBusCapacity();
        long getTicketsSold();
        long getBookingCount();
        BigDecimal getTicketSalesAmount();
    }

    interface ScheduleOccupancyProjection {
        Long getScheduleId();
        String getRoute();
        LocalDate getTravelDate();
        LocalTime getDepartureTime();
        String getBusRegistrationNo();
        int getBusCapacity();
        long getOccupiedSeats();
    }
}
