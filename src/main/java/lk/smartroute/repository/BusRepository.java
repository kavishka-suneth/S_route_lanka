package lk.smartroute.repository;

import lk.smartroute.model.AvailabilityStatus;
import lk.smartroute.model.Bus;
import lk.smartroute.model.BusStatus;
import lk.smartroute.model.BusType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus, Long> {
    Optional<Bus> findByRegistrationNoIgnoreCase(String registrationNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bus b where b.id = :id")
    Optional<Bus> findByIdForUpdate(@Param("id") Long id);

    boolean existsByRegistrationNoIgnoreCase(String registrationNo);

    Page<Bus> findByStatus(BusStatus status, Pageable pageable);

    Page<Bus> findByStatusNot(BusStatus status, Pageable pageable);

    @Query("""
        select b
        from Bus b
        where (:active is null
               or (:active = true and b.status <> lk.smartroute.model.BusStatus.INACTIVE)
               or (:active = false and b.status = lk.smartroute.model.BusStatus.INACTIVE))
          and (:availabilityStatus is null or b.availabilityStatus = :availabilityStatus)
        """)
    Page<Bus> searchForManagement(
        @Param("active") Boolean active,
        @Param("availabilityStatus") AvailabilityStatus availabilityStatus,
        Pageable pageable
    );

    List<Bus> findByAvailabilityStatus(AvailabilityStatus availabilityStatus);

    List<Bus> findByBusType(BusType busType);

    List<Bus> findByCapacityGreaterThanEqual(int requiredPassengerCount);

    @Query("""
        select b
        from Bus b
        where b.status = lk.smartroute.model.BusStatus.ACTIVE
          and b.availabilityStatus = lk.smartroute.model.AvailabilityStatus.AVAILABLE
          and b.id not in (
            select s.bus.id
            from Schedule s
            where (:excludeScheduleId is null or s.id <> :excludeScheduleId)
              and s.status in (
                  lk.smartroute.model.ScheduleStatus.SCHEDULED,
                  lk.smartroute.model.ScheduleStatus.DELAYED
              )
              and s.travelDate = :travelDate
              and :newDeparture < s.arrivalTime
              and :newArrival > s.departureTime
        )
        order by b.registrationNo
        """)
    List<Bus> findAvailableDuring(
        @Param("travelDate") LocalDate travelDate,
        @Param("newDeparture") LocalTime newDeparture,
        @Param("newArrival") LocalTime newArrival,
        @Param("excludeScheduleId") Long excludeScheduleId
    );

    @Query("select count(seat) from Seat seat where seat.bus.id = :busId")
    long countSeatsByBusId(@Param("busId") Long busId);

    @Query("""
        select count(sc)
        from Schedule sc
        where sc.bus.id = :busId
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
          and (sc.travelDate > :today or (sc.travelDate = :today and sc.departureTime >= :time))
        """)
    long countFutureActiveSchedules(@Param("busId") Long busId, @Param("today") LocalDate today, @Param("time") LocalTime time);

}
