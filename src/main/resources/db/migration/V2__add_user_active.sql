-- Add real account activation state used by login and administration.
ALTER TABLE users
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE AFTER user_type;

CREATE INDEX idx_users_active ON users (active);
