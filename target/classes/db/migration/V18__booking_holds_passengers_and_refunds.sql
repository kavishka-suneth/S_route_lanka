ALTER TABLE bookings ADD COLUMN payment_due_at DATETIME(6) NULL;
CREATE INDEX idx_bookings_payment_due ON bookings (booking_status, payment_due_at);

ALTER TABLE payments ADD COLUMN refunded_amount DECIMAL(10,2) NULL;

CREATE TABLE booking_passengers (
  booking_passenger_id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  booking_id BIGINT NOT NULL,
  seat_number INT NOT NULL,
  full_name VARCHAR(160) NOT NULL,
  nic_or_passport VARCHAR(40) NULL,
  phone VARCHAR(30) NULL,
  email VARCHAR(160) NULL,
  PRIMARY KEY (booking_passenger_id),
  CONSTRAINT uk_booking_passenger_seat UNIQUE (booking_id, seat_number),
  CONSTRAINT fk_booking_passenger_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX idx_booking_passengers_booking ON booking_passengers (booking_id);
