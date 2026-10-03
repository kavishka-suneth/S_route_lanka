ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE AFTER active;

UPDATE users
SET email_verified = TRUE;

CREATE INDEX idx_users_email_verified ON users (email_verified);
