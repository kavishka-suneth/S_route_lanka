CREATE TABLE wallets (
  wallet_id BIGINT NOT NULL AUTO_INCREMENT, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  user_id BIGINT NOT NULL, balance DECIMAL(19,2) NOT NULL DEFAULT 0.00, version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (wallet_id), CONSTRAINT uk_wallets_user UNIQUE (user_id),
  CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT chk_wallet_balance CHECK (balance >= 0)
) ENGINE=InnoDB;

CREATE TABLE customer_cards (
  card_id BIGINT NOT NULL AUTO_INCREMENT, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  user_id BIGINT NOT NULL, cardholder_name VARCHAR(120) NOT NULL, brand VARCHAR(20) NOT NULL,
  last_four VARCHAR(4) NOT NULL, expiry_month INT NOT NULL, expiry_year INT NOT NULL,
  card_token VARCHAR(100) NOT NULL, verified BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY(card_id), CONSTRAINT uk_customer_cards_token UNIQUE(card_token),
  CONSTRAINT fk_customer_cards_user FOREIGN KEY(user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;
CREATE INDEX idx_customer_cards_user ON customer_cards(user_id);

CREATE TABLE otp_verifications (
  otp_id BIGINT NOT NULL AUTO_INCREMENT, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  user_id BIGINT NULL, email VARCHAR(160) NOT NULL, purpose VARCHAR(40) NOT NULL,
  operation_key VARCHAR(120) NOT NULL, otp_hash VARCHAR(255) NOT NULL, payload_json TEXT NULL,
  expires_at DATETIME(6) NOT NULL, last_sent_at DATETIME(6) NOT NULL, attempts INT NOT NULL DEFAULT 0,
  used BOOLEAN NOT NULL DEFAULT FALSE, verified BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY(otp_id), CONSTRAINT fk_otp_user FOREIGN KEY(user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;
CREATE INDEX idx_otp_lookup ON otp_verifications(email,purpose,operation_key,used);

CREATE TABLE wallet_transactions (
  wallet_transaction_id BIGINT NOT NULL AUTO_INCREMENT, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  wallet_id BIGINT NOT NULL, booking_id BIGINT NULL, card_id BIGINT NULL,
  transaction_reference VARCHAR(100) NOT NULL, transaction_type VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL, amount DECIMAL(19,2) NOT NULL, balance_after DECIMAL(19,2) NOT NULL,
  related_reference VARCHAR(100) NULL,
  PRIMARY KEY(wallet_transaction_id), CONSTRAINT uk_wallet_tx_reference UNIQUE(transaction_reference),
  CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY(wallet_id) REFERENCES wallets(wallet_id),
  CONSTRAINT fk_wallet_tx_booking FOREIGN KEY(booking_id) REFERENCES bookings(booking_id),
  CONSTRAINT fk_wallet_tx_card FOREIGN KEY(card_id) REFERENCES customer_cards(card_id) ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE INDEX idx_wallet_tx_wallet_date ON wallet_transactions(wallet_id,created_at);
CREATE UNIQUE INDEX uk_wallet_booking_payment ON wallet_transactions(booking_id,transaction_type);

INSERT INTO wallets(created_at,updated_at,user_id,balance,version)
SELECT CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6),u.user_id,0.00,0 FROM users u
WHERE u.user_type='PASSENGER' AND NOT EXISTS (SELECT 1 FROM wallets w WHERE w.user_id=u.user_id);
