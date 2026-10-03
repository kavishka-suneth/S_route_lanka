package lk.smartroute.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
    indexes = {
        @Index(name = "idx_users_user_type", columnList = "user_type"),
        @Index(name = "idx_users_name_email", columnList = "first_name,last_name,email")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "user_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(length = 30)
    private String phone;

    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 30)
    private UserType userType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @OneToOne(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Passenger passenger;

    @OneToOne(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Staff staff;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Role getRole() {
        if (userType == UserType.PASSENGER) {
            return Role.PASSENGER;
        }
        return staff == null ? Role.CUSTOMER_SERVICE : Role.fromStaffType(staff.getStaffType());
    }
}
