package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_user", columnList = "user_id"),
        @Index(name = "idx_notifications_type", columnList = "notification_type"),
        @Index(name = "idx_notifications_status", columnList = "status"),
        @Index(name = "idx_notifications_sent", columnList = "sent_date_time"),
        @Index(name = "idx_notifications_category", columnList = "category"),
        @Index(name = "idx_notifications_entity", columnList = "related_entity_type,related_entity_id")
    }
)
@AttributeOverride(name = "id", column = @Column(name = "notification_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false, length = 160)
    private String title = "Notification";

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationCategory category = NotificationCategory.BOOKING_UPDATES;

    /** Channels requested for this notification. Providers are currently mocked/logged. */
    @Column(name = "delivery_channels", nullable = false, length = 50)
    private String deliveryChannels = "IN_APP";

    @Column(name = "related_reference", length = 100)
    private String relatedReference;

    @Column(name = "related_entity_type", length = 60)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "action_url", length = 300)
    private String actionUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(name = "sent_date_time", nullable = false)
    private LocalDateTime sentDateTime;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(nullable = false)
    private boolean archived = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationStatus status = NotificationStatus.CREATED;
}
