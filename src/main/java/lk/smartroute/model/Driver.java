package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "drivers",
    uniqueConstraints = @UniqueConstraint(name = "uk_drivers_license_no", columnNames = "license_no"),
    indexes = {
        @Index(name = "idx_drivers_availability", columnList = "availability_status"),
        @Index(name = "idx_drivers_name_license", columnList = "first_name,last_name,license_no")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "driver_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Driver extends BaseEntity {
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "license_no", nullable = false, length = 60)
    private String licenseNo;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 30)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
