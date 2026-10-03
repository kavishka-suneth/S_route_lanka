package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "passengers",
    uniqueConstraints = @UniqueConstraint(name = "uk_passengers_user", columnNames = "user_id")
)
@AttributeOverride(name = "id", column = @Column(name = "passenger_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Passenger extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
