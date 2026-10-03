package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "booking_seats",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_booking_seats_schedule_seat", columnNames = {"schedule_id", "seat_id"}),
        @UniqueConstraint(name = "uk_booking_seats_booking_seat", columnNames = {"booking_id", "seat_id"})
    },
    indexes = {
        @Index(name = "idx_booking_seats_booking", columnList = "booking_id"),
        @Index(name = "idx_booking_seats_schedule", columnList = "schedule_id"),
        @Index(name = "idx_booking_seats_seat", columnList = "seat_id")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "booking_seat_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class BookingSeat extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    public BookingSeat(Booking booking, Seat seat, Schedule schedule) {
        this.booking = booking;
        this.seat = seat;
        this.schedule = schedule;
    }
}
