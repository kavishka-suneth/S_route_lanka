package lk.smartroute.repository;

import lk.smartroute.model.AvailabilityStatus;
import lk.smartroute.model.Driver;
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

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByLicenseNoIgnoreCase(String licenseNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Driver d where d.id = :id")
    Optional<Driver> findByIdForUpdate(@Param("id") Long id);

    boolean existsByLicenseNoIgnoreCase(String licenseNo);

    List<Driver> findByAvailabilityStatus(AvailabilityStatus availabilityStatus);

    Page<Driver> findByAvailabilityStatus(AvailabilityStatus availabilityStatus, Pageable pageable);

    @Query("""
        select d
        from Driver d
        where (:active is null
               or (:active = true and d.availabilityStatus = lk.smartroute.model.AvailabilityStatus.AVAILABLE)
               or (:active = false and d.availabilityStatus <> lk.smartroute.model.AvailabilityStatus.AVAILABLE))
          and (:availabilityStatus is null or d.availabilityStatus = :availabilityStatus)
        """)
    Page<Driver> searchForManagement(
        @Param("active") Boolean active,
        @Param("availabilityStatus") AvailabilityStatus availabilityStatus,
        Pageable pageable
    );

    @Query("""
        select d
        from Driver d
        where lower(d.firstName) like lower(concat('%', :keyword, '%'))
           or lower(d.lastName) like lower(concat('%', :keyword, '%'))
           or lower(d.licenseNo) like lower(concat('%', :keyword, '%'))
        """)
    List<Driver> searchDrivers(@Param("keyword") String keyword);

    @Query("""
        select d
        from Driver d
        where d.availabilityStatus = lk.smartroute.model.AvailabilityStatus.AVAILABLE
          and d.id not in (
            select s.driver.id
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
        order by d.firstName, d.lastName
        """)
    List<Driver> findAvailableDuring(
        @Param("travelDate") LocalDate travelDate,
        @Param("newDeparture") LocalTime newDeparture,
        @Param("newArrival") LocalTime newArrival,
        @Param("excludeScheduleId") Long excludeScheduleId
    );

    @Query("""
        select count(sc)
        from Schedule sc
        where sc.driver.id = :driverId
          and sc.status in (
              lk.smartroute.model.ScheduleStatus.SCHEDULED,
              lk.smartroute.model.ScheduleStatus.DELAYED
          )
          and (sc.travelDate > :today or (sc.travelDate = :today and sc.departureTime >= :time))
        """)
    long countFutureActiveSchedules(@Param("driverId") Long driverId, @Param("today") LocalDate today, @Param("time") LocalTime time);
}
