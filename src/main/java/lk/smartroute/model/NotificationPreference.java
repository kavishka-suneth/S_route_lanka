package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "notification_preferences",
    uniqueConstraints = @UniqueConstraint(name = "uk_notification_preferences_user", columnNames = "user_id")
)
@AttributeOverride(name = "id", column = @Column(name = "notification_preference_id", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreference extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = false;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "booking_updates_enabled", nullable = false)
    private boolean bookingUpdatesEnabled = true;

    @Column(name = "payment_updates_enabled", nullable = false)
    private boolean paymentUpdatesEnabled = true;

    @Column(name = "schedule_changes_enabled", nullable = false)
    private boolean scheduleChangesEnabled = true;

    @Column(name = "trip_reminders_enabled", nullable = false)
    private boolean tripRemindersEnabled = true;

    @Column(name = "wallet_updates_enabled", nullable = false)
    private boolean walletUpdatesEnabled = true;

    @Column(name = "support_messages_enabled", nullable = false)
    private boolean supportMessagesEnabled = true;

    @Column(name = "security_alerts_enabled", nullable = false)
    private boolean securityAlertsEnabled = true;

    @Column(name = "promotional_enabled", nullable = false)
    private boolean promotionalEnabled = false;
}
