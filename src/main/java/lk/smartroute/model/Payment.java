package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "payments",
    uniqueConstraints = @UniqueConstraint(name = "uk_payments_transaction_reference", columnNames = "transaction_reference"),
    indexes = {
        @Index(name = "idx_payments_booking", columnList = "booking_id"),
        @Index(name = "idx_payments_status_date", columnList = "payment_status,payment_date"),
        @Index(name = "idx_payments_method", columnList = "payment_method")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "payment_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "payment_method", nullable = false, length = 40)
    private String paymentMethod;

    @Column(name = "transaction_reference", nullable = false, length = 100)
    private String transactionReference;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "refunded_amount", precision = 10, scale = 2)
    private BigDecimal refundedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
}
