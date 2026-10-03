package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(
    name = "bookings",
    uniqueConstraints = @UniqueConstraint(name = "uk_bookings_reference", columnNames = "booking_reference"),
    indexes = {
        @Index(name = "idx_bookings_passenger", columnList = "passenger_id"),
        @Index(name = "idx_bookings_schedule_status", columnList = "schedule_id,booking_status"),
        @Index(name = "idx_bookings_date", columnList = "booking_date"),
        @Index(name = "idx_bookings_payment_due", columnList = "booking_status,payment_due_at")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "booking_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Booking extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 30)
    private BookingStatus bookingStatus = BookingStatus.PENDING_PAYMENT;

    @Column(name = "payment_due_at")
    private LocalDateTime paymentDueAt;

    @Column(name = "payment_deadline_warning_sent_at")
    private LocalDateTime paymentDeadlineWarningSentAt;

    @Column(name = "expired_notification_sent_at")
    private LocalDateTime expiredNotificationSentAt;

    @Column(name = "reminder_24h_sent_at")
    private LocalDateTime reminder24hSentAt;

    @Column(name = "reminder_2h_sent_at")
    private LocalDateTime reminder2hSentAt;

    @Column(name = "reminder_30m_sent_at")
    private LocalDateTime reminder30mSentAt;

    @Column(name = "passenger_count", nullable = false)
    private int passengerCount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "booking_reference", nullable = false, length = 40)
    private String bookingReference;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seatNumber ASC")
    private List<BookingPassenger> passengerDetails = new ArrayList<>();

    public void replaceSeats(Schedule schedule, Collection<Seat> seats) {
        bookingSeats.clear();
        seats.forEach(seat -> bookingSeats.add(new BookingSeat(this, seat, schedule)));
        passengerCount = seats.size();
    }

    public void releaseSeats() {
        bookingSeats.clear();
    }

    public void replacePassengerDetails(Collection<BookingPassenger> details) {
        passengerDetails.clear();
        passengerDetails.addAll(details);
    }
}
