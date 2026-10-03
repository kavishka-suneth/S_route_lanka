CREATE TABLE notification_preferences (
  notification_preference_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  user_id BIGINT NOT NULL,
  email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (notification_preference_id),
  CONSTRAINT uk_notification_preferences_user UNIQUE (user_id),
  CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

ALTER TABLE notifications ADD COLUMN related_reference VARCHAR(100) NULL;
CREATE INDEX idx_notifications_type_reference ON notifications (notification_type, related_reference);
