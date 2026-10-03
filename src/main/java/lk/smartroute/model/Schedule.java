package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
    name = "schedules",
    indexes = {
        @Index(name = "idx_schedules_route_date_status", columnList = "route_id,travel_date,status"),
        @Index(name = "idx_schedules_bus_time", columnList = "bus_id,travel_date,departure_time,arrival_time"),
        @Index(name = "idx_schedules_driver_time", columnList = "driver_id,travel_date,departure_time,arrival_time"),
        @Index(name = "idx_schedules_status", columnList = "status")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "schedule_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Schedule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScheduleStatus status = ScheduleStatus.SCHEDULED;

    @Column(name = "delay_reason", length = 500)
    private String delayReason;

    @Column(name = "original_departure_time")
    private LocalTime originalDepartureTime;

    @Column(name = "last_delay_notified_at")
    private LocalDateTime lastDelayNotifiedAt;

    @Column(name = "last_update_notified_at")
    private LocalDateTime lastUpdateNotifiedAt;

    public LocalDateTime getDepartureDateTime() {
        return LocalDateTime.of(travelDate, departureTime);
    }

    public LocalDateTime getArrivalDateTime() {
        return LocalDateTime.of(travelDate, arrivalTime);
    }
}
