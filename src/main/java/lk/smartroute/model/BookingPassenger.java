package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_passengers", uniqueConstraints = @UniqueConstraint(name = "uk_booking_passenger_seat", columnNames = {"booking_id", "seat_number"}), indexes = @Index(name = "idx_booking_passengers_booking", columnList = "booking_id"))
@AttributeOverride(name = "id", column = @Column(name = "booking_passenger_id", nullable = false, updatable = false))
@Getter @Setter @NoArgsConstructor
public class BookingPassenger extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "nic_or_passport", length = 40)
    private String nicOrPassport;

    @Column(length = 30)
    private String phone;

    @Column(length = 160)
    private String email;

    public BookingPassenger(Booking booking, int seatNumber, String fullName, String nicOrPassport, String phone, String email) {
        this.booking = booking; this.seatNumber = seatNumber; this.fullName = fullName;
        this.nicOrPassport = nicOrPassport; this.phone = phone; this.email = email;
    }
}
