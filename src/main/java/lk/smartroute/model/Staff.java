package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(
    name = "staff",
    uniqueConstraints = @UniqueConstraint(name = "uk_staff_user", columnNames = "user_id"),
    indexes = @Index(name = "idx_staff_staff_type", columnList = "staff_type")
)
@AttributeOverride(name = "id", column = @Column(name = "staff_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Staff extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false, length = 50)
    private StaffType staffType;
}
