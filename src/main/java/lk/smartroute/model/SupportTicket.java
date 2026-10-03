package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "support_tickets",
    indexes = {
        @Index(name = "idx_support_tickets_user", columnList = "user_id"),
        @Index(name = "idx_support_tickets_staff", columnList = "handled_by_staff_id"),
        @Index(name = "idx_support_tickets_status", columnList = "status"),
        @Index(name = "idx_support_tickets_created", columnList = "created_date")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "ticket_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class SupportTicket extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_staff_id")
    private Staff handledByStaff;

    @Column(nullable = false, length = 160)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}
