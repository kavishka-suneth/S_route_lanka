CREATE TABLE users (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  email VARCHAR(160) NOT NULL,
  phone VARCHAR(30),
  password VARCHAR(255) NOT NULL,
  user_type VARCHAR(30) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  email_verified BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (user_id),
  CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE INDEX idx_users_user_type ON users (user_type);
CREATE INDEX idx_users_name_email ON users (first_name, last_name, email);
CREATE INDEX idx_users_active ON users (active);
CREATE INDEX idx_users_email_verified ON users (email_verified);

CREATE TABLE passengers (
  passenger_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (passenger_id),
  CONSTRAINT uk_passengers_user UNIQUE (user_id),
  CONSTRAINT fk_passengers_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE staff (
  staff_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  user_id BIGINT NOT NULL,
  hire_date DATE NOT NULL,
  staff_type VARCHAR(50) NOT NULL,
  PRIMARY KEY (staff_id),
  CONSTRAINT uk_staff_user UNIQUE (user_id),
  CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_staff_staff_type ON staff (staff_type);

CREATE TABLE routes (
  route_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  origin VARCHAR(120) NOT NULL,
  destination VARCHAR(120) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (route_id),
  CONSTRAINT uk_routes_origin_destination UNIQUE (origin, destination)
) ENGINE=InnoDB;

CREATE INDEX idx_routes_origin_destination ON routes (origin, destination);
CREATE INDEX idx_routes_active ON routes (active);

CREATE TABLE buses (
  bus_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  registration_no VARCHAR(30) NOT NULL,
  capacity INT NOT NULL,
  bus_type VARCHAR(30) NOT NULL,
  availability_status VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL,
  PRIMARY KEY (bus_id),
  CONSTRAINT uk_buses_registration_no UNIQUE (registration_no)
) ENGINE=InnoDB;

CREATE INDEX idx_buses_availability ON buses (availability_status);
CREATE INDEX idx_buses_bus_type ON buses (bus_type);
CREATE INDEX idx_buses_capacity ON buses (capacity);

CREATE TABLE seats (
  seat_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  bus_id BIGINT NOT NULL,
  seat_number INT NOT NULL,
  seat_type VARCHAR(30) NOT NULL,
  PRIMARY KEY (seat_id),
  CONSTRAINT uk_seats_bus_number UNIQUE (bus_id, seat_number),
  CONSTRAINT fk_seats_bus FOREIGN KEY (bus_id) REFERENCES buses (bus_id)
) ENGINE=InnoDB;

CREATE INDEX idx_seats_bus ON seats (bus_id);

CREATE TABLE drivers (
  driver_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  license_no VARCHAR(60) NOT NULL,
  phone VARCHAR(30),
  availability_status VARCHAR(30) NOT NULL,
  PRIMARY KEY (driver_id),
  CONSTRAINT uk_drivers_license_no UNIQUE (license_no)
) ENGINE=InnoDB;

CREATE INDEX idx_drivers_availability ON drivers (availability_status);
CREATE INDEX idx_drivers_name_license ON drivers (first_name, last_name, license_no);

CREATE TABLE schedules (
  schedule_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  route_id BIGINT NOT NULL,
  bus_id BIGINT NOT NULL,
  driver_id BIGINT NOT NULL,
  travel_date DATE NOT NULL,
  departure_time TIME NOT NULL,
  arrival_time TIME NOT NULL,
  fare DECIMAL(10,2) NOT NULL,
  status VARCHAR(30) NOT NULL,
  PRIMARY KEY (schedule_id),
  CONSTRAINT fk_schedules_route FOREIGN KEY (route_id) REFERENCES routes (route_id),
  CONSTRAINT fk_schedules_bus FOREIGN KEY (bus_id) REFERENCES buses (bus_id),
  CONSTRAINT fk_schedules_driver FOREIGN KEY (driver_id) REFERENCES drivers (driver_id)
) ENGINE=InnoDB;

CREATE INDEX idx_schedules_route_date_status ON schedules (route_id, travel_date, status);
CREATE INDEX idx_schedules_bus_time ON schedules (bus_id, travel_date, departure_time, arrival_time);
CREATE INDEX idx_schedules_driver_time ON schedules (driver_id, travel_date, departure_time, arrival_time);
CREATE INDEX idx_schedules_bus_assignment ON schedules (bus_id, travel_date, status, departure_time, arrival_time);
CREATE INDEX idx_schedules_driver_assignment ON schedules (driver_id, travel_date, status, departure_time, arrival_time);
CREATE INDEX idx_schedules_status ON schedules (status);

CREATE TABLE bookings (
  booking_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  passenger_id BIGINT NOT NULL,
  schedule_id BIGINT NOT NULL,
  booking_date DATETIME(6) NOT NULL,
  booking_status VARCHAR(30) NOT NULL,
  passenger_count INT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  booking_reference VARCHAR(40) NOT NULL,
  PRIMARY KEY (booking_id),
  CONSTRAINT uk_bookings_reference UNIQUE (booking_reference),
  CONSTRAINT fk_bookings_passenger FOREIGN KEY (passenger_id) REFERENCES passengers (passenger_id),
  CONSTRAINT fk_bookings_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (schedule_id)
) ENGINE=InnoDB;

CREATE INDEX idx_bookings_passenger ON bookings (passenger_id);
CREATE INDEX idx_bookings_schedule_status ON bookings (schedule_id, booking_status);
CREATE INDEX idx_bookings_date ON bookings (booking_date);

CREATE TABLE booking_seats (
  booking_seat_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  booking_id BIGINT NOT NULL,
  seat_id BIGINT NOT NULL,
  schedule_id BIGINT NOT NULL,
  PRIMARY KEY (booking_seat_id),
  CONSTRAINT uk_booking_seats_schedule_seat UNIQUE (schedule_id, seat_id),
  CONSTRAINT uk_booking_seats_booking_seat UNIQUE (booking_id, seat_id),
  CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id),
  CONSTRAINT fk_booking_seats_seat FOREIGN KEY (seat_id) REFERENCES seats (seat_id),
  CONSTRAINT fk_booking_seats_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (schedule_id)
) ENGINE=InnoDB;

CREATE INDEX idx_booking_seats_booking ON booking_seats (booking_id);
CREATE INDEX idx_booking_seats_schedule ON booking_seats (schedule_id);
CREATE INDEX idx_booking_seats_seat ON booking_seats (seat_id);

CREATE TABLE payments (
  payment_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  booking_id BIGINT NOT NULL,
  payment_method VARCHAR(40) NOT NULL,
  transaction_reference VARCHAR(100) NOT NULL,
  payment_date DATETIME(6) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  payment_status VARCHAR(30) NOT NULL,
  PRIMARY KEY (payment_id),
  CONSTRAINT uk_payments_transaction_reference UNIQUE (transaction_reference),
  CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
) ENGINE=InnoDB;

CREATE INDEX idx_payments_booking ON payments (booking_id);
CREATE INDEX idx_payments_status_date ON payments (payment_status, payment_date);
CREATE INDEX idx_payments_method ON payments (payment_method);

CREATE TABLE reviews (
  review_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  passenger_id BIGINT NOT NULL,
  booking_id BIGINT NOT NULL,
  rating INT NOT NULL,
  comment VARCHAR(1000),
  review_date DATETIME(6) NOT NULL,
  PRIMARY KEY (review_id),
  CONSTRAINT uk_reviews_booking UNIQUE (booking_id),
  CONSTRAINT fk_reviews_passenger FOREIGN KEY (passenger_id) REFERENCES passengers (passenger_id),
  CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
) ENGINE=InnoDB;

CREATE INDEX idx_reviews_passenger ON reviews (passenger_id);
CREATE INDEX idx_reviews_rating ON reviews (rating);
CREATE INDEX idx_reviews_date ON reviews (review_date);

CREATE TABLE support_tickets (
  ticket_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  user_id BIGINT NOT NULL,
  handled_by_staff_id BIGINT,
  subject VARCHAR(160) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_date DATETIME(6) NOT NULL,
  PRIMARY KEY (ticket_id),
  CONSTRAINT fk_support_tickets_user FOREIGN KEY (user_id) REFERENCES users (user_id),
  CONSTRAINT fk_support_tickets_staff FOREIGN KEY (handled_by_staff_id) REFERENCES staff (staff_id)
) ENGINE=InnoDB;

CREATE INDEX idx_support_tickets_user ON support_tickets (user_id);
CREATE INDEX idx_support_tickets_staff ON support_tickets (handled_by_staff_id);
CREATE INDEX idx_support_tickets_status ON support_tickets (status);
CREATE INDEX idx_support_tickets_created ON support_tickets (created_date);

CREATE TABLE notifications (
  notification_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  user_id BIGINT NOT NULL,
  notification_type VARCHAR(50) NOT NULL,
  message VARCHAR(2000) NOT NULL,
  delivery_channels VARCHAR(50) NOT NULL DEFAULT 'IN_APP',
  sent_date_time DATETIME(6) NOT NULL,
  status VARCHAR(30) NOT NULL,
  PRIMARY KEY (notification_id),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE INDEX idx_notifications_user ON notifications (user_id);
CREATE INDEX idx_notifications_type ON notifications (notification_type);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_sent ON notifications (sent_date_time);

-- SmartRoute Wallet / card / OTP schema (matches Flyway V6)
CREATE TABLE wallets (
  wallet_id BIGINT NOT NULL AUTO_INCREMENT, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  user_id BIGINT NOT NULL, balance DECIMAL(19,2) NOT NULL DEFAULT 0.00, version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (wallet_id), CONSTRAINT uk_wallets_user UNIQUE (user_id),
  CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT chk_wallet_balance CHECK (balance >= 0)
) ENGINE=InnoDB;

CREATE TABLE customer_cards (
  card_id BIGINT NOT NULL AUTO_INCREMENT, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  user_id BIGINT NOT NULL, cardholder_name VARCHAR(120) NOT NULL, card_brand VARCHAR(20) NOT NULL,
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
  operation_id VARCHAR(120) NULL,
  resend_available_at DATETIME(6) NULL,
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
