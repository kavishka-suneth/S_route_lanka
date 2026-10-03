package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "routes",
    uniqueConstraints = @UniqueConstraint(name = "uk_routes_origin_destination", columnNames = {"origin", "destination"}),
    indexes = {
        @Index(name = "idx_routes_origin_destination", columnList = "origin,destination"),
        @Index(name = "idx_routes_active", columnList = "active")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "route_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Route extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String origin;

    @Column(nullable = false, length = 120)
    private String destination;

    @Column(nullable = false)
    private boolean active = true;
}
