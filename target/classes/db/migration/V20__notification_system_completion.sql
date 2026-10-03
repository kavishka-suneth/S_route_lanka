ALTER TABLE notifications
  ADD COLUMN title VARCHAR(160) NOT NULL DEFAULT 'Notification' AFTER notification_type,
  ADD COLUMN category VARCHAR(40) NOT NULL DEFAULT 'BOOKING_UPDATES' AFTER message,
  ADD COLUMN related_entity_type VARCHAR(60) NULL AFTER related_reference,
  ADD COLUMN related_entity_id BIGINT NULL AFTER related_entity_type,
  ADD COLUMN action_url VARCHAR(300) NULL AFTER related_entity_id,
  ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER action_url,
  ADD COLUMN read_at DATETIME(6) NULL AFTER sent_date_time,
  ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE AFTER read_at;

CREATE INDEX idx_notifications_category ON notifications (category);
CREATE INDEX idx_notifications_entity ON notifications (related_entity_type, related_entity_id);
CREATE INDEX idx_notifications_user_type_reference ON notifications (user_id, notification_type, related_reference);

ALTER TABLE notification_preferences
  ADD COLUMN booking_updates_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN payment_updates_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN schedule_changes_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN trip_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN wallet_updates_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN support_messages_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN security_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN promotional_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE bookings
  ADD COLUMN payment_deadline_warning_sent_at DATETIME(6) NULL,
  ADD COLUMN expired_notification_sent_at DATETIME(6) NULL,
  ADD COLUMN reminder_24h_sent_at DATETIME(6) NULL,
  ADD COLUMN reminder_2h_sent_at DATETIME(6) NULL,
  ADD COLUMN reminder_30m_sent_at DATETIME(6) NULL;

CREATE INDEX idx_bookings_reminder_24h ON bookings (booking_status, reminder_24h_sent_at);
CREATE INDEX idx_bookings_reminder_2h ON bookings (booking_status, reminder_2h_sent_at);
CREATE INDEX idx_bookings_payment_warning ON bookings (booking_status, payment_due_at, payment_deadline_warning_sent_at);

ALTER TABLE schedules
  ADD COLUMN delay_reason VARCHAR(500) NULL,
  ADD COLUMN original_departure_time TIME NULL,
  ADD COLUMN last_delay_notified_at DATETIME(6) NULL,
  ADD COLUMN last_update_notified_at DATETIME(6) NULL;
