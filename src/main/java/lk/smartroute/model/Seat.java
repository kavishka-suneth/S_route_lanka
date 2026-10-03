package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = @UniqueConstraint(name = "uk_seats_bus_number", columnNames = {"bus_id", "seat_number"}),
    indexes = @Index(name = "idx_seats_bus", columnList = "bus_id")
)
@AttributeOverride(name = "id", column = @Column(name = "seat_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Seat extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 30)
    private SeatType seatType = SeatType.REGULAR;
}
