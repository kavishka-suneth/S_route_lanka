package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "buses",
    uniqueConstraints = @UniqueConstraint(name = "uk_buses_registration_no", columnNames = "registration_no"),
    indexes = {
        @Index(name = "idx_buses_availability", columnList = "availability_status"),
        @Index(name = "idx_buses_bus_type", columnList = "bus_type"),
        @Index(name = "idx_buses_capacity", columnList = "capacity")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "bus_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Bus extends BaseEntity {
    @Column(name = "registration_no", nullable = false, length = 30)
    private String registrationNo;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "bus_type", nullable = false, length = 30)
    private BusType busType;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 30)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BusStatus status = BusStatus.ACTIVE;

    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();
}
