-- Adds the channel audit field used by the notification and support workflow.
ALTER TABLE notifications
    ADD COLUMN delivery_channels VARCHAR(50) NOT NULL DEFAULT 'IN_APP' AFTER message;
