package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(name = "uk_reviews_booking", columnNames = "booking_id"),
    indexes = {
        @Index(name = "idx_reviews_passenger", columnList = "passenger_id"),
        @Index(name = "idx_reviews_rating", columnList = "rating"),
        @Index(name = "idx_reviews_date", columnList = "review_date")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "review_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Review extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    @Column(name = "review_date", nullable = false)
    private LocalDateTime reviewDate;
}
