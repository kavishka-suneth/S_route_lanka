-- SmartRoute Lanka - Sample data for MySQL 8 / Flyway
-- Matches src/main/resources/db/migration/V1__init_schema.sql
-- All sample accounts use the password: Password@123
-- BCrypt hash below is safe for DEVELOPMENT/DEMO use only.

-- =========================================================
-- 1. USERS: PASSENGERS + EVERY STAFF ROLE
-- =========================================================

INSERT INTO users
    (created_at, updated_at, first_name, last_name, email, phone, password, user_type)
VALUES
    (NOW(6), NOW(6), 'Kavindu', 'Perera', 'passenger1@smartroute.lk', '0711111111',
     '$2b$12$5YrRG7dTqZmlnf96J1lVPODQL3K1jeQp4CA8j5adPZuS3Kacwtqne', 'PASSENGER'),
    (NOW(6), NOW(6), 'Nethmi', 'Silva', 'passenger2@smartroute.lk', '0722222222',
     '$2b$12$5YrRG7dTqZmlnf96J1lVPODQL3K1jeQp4CA8j5adPZuS3Kacwtqne', 'PASSENGER'),
    (NOW(6), NOW(6), 'Amali', 'Fernando', 'customer.service@smartroute.lk', '0733333333',
     '$2b$12$5YrRG7dTqZmlnf96J1lVPODQL3K1jeQp4CA8j5adPZuS3Kacwtqne', 'STAFF'),
    (NOW(6), NOW(6), 'Kasun', 'Bandara', 'coordinator@smartroute.lk', '0744444444',
     '$2b$12$5YrRG7dTqZmlnf96J1lVPODQL3K1jeQp4CA8j5adPZuS3Kacwtqne', 'STAFF'),
    (NOW(6), NOW(6), 'Dinithi', 'Jayasinghe', 'finance@smartroute.lk', '0755555555',
     '$2b$12$5YrRG7dTqZmlnf96J1lVPODQL3K1jeQp4CA8j5adPZuS3Kacwtqne', 'STAFF'),
    (NOW(6), NOW(6), 'Ruwan', 'Gunasekara', 'operations@smartroute.lk', '0766666666',
     '$2b$12$5YrRG7dTqZmlnf96J1lVPODQL3K1jeQp4CA8j5adPZuS3Kacwtqne', 'STAFF'),
    (NOW(6), NOW(6), 'System', 'Administrator', 'admin@smartroute.lk', '0777777777',
     '$2b$12$5YrRG7dTqZmlnf96J1lVPODQL3K1jeQp4CA8j5adPZuS3Kacwtqne', 'STAFF'),
    (NOW(6), NOW(6), 'Tharindu', 'Wijesinghe', 'itsupport@smartroute.lk', '0788888888',
     '$2b$12$5YrRG7dTqZmlnf96J1lVPODQL3K1jeQp4CA8j5adPZuS3Kacwtqne', 'STAFF');

INSERT INTO passengers (created_at, updated_at, user_id)
SELECT NOW(6), NOW(6), user_id
FROM users
WHERE email IN ('passenger1@smartroute.lk', 'passenger2@smartroute.lk');

INSERT INTO staff (created_at, updated_at, user_id, hire_date, staff_type)
SELECT NOW(6), NOW(6), user_id, DATE_SUB(CURDATE(), INTERVAL 18 MONTH),
       'CUSTOMER_SERVICE_STAFF'
FROM users WHERE email = 'customer.service@smartroute.lk';

INSERT INTO staff (created_at, updated_at, user_id, hire_date, staff_type)
SELECT NOW(6), NOW(6), user_id, DATE_SUB(CURDATE(), INTERVAL 2 YEAR),
       'TRANSPORT_COORDINATOR'
FROM users WHERE email = 'coordinator@smartroute.lk';

INSERT INTO staff (created_at, updated_at, user_id, hire_date, staff_type)
SELECT NOW(6), NOW(6), user_id, DATE_SUB(CURDATE(), INTERVAL 20 MONTH),
       'FINANCE_OFFICER'
FROM users WHERE email = 'finance@smartroute.lk';

INSERT INTO staff (created_at, updated_at, user_id, hire_date, staff_type)
SELECT NOW(6), NOW(6), user_id, DATE_SUB(CURDATE(), INTERVAL 3 YEAR),
       'OPERATIONS_MANAGER'
FROM users WHERE email = 'operations@smartroute.lk';

INSERT INTO staff (created_at, updated_at, user_id, hire_date, staff_type)
SELECT NOW(6), NOW(6), user_id, DATE_SUB(CURDATE(), INTERVAL 4 YEAR),
       'SYSTEM_ADMINISTRATOR'
FROM users WHERE email = 'admin@smartroute.lk';

INSERT INTO staff (created_at, updated_at, user_id, hire_date, staff_type)
SELECT NOW(6), NOW(6), user_id, DATE_SUB(CURDATE(), INTERVAL 1 YEAR),
       'IT_SUPPORT_STAFF'
FROM users WHERE email = 'itsupport@smartroute.lk';

-- =========================================================
-- 2. ROUTES, BUSES, SEATS AND DRIVERS
-- =========================================================

INSERT INTO routes (created_at, updated_at, origin, destination)
VALUES
    (NOW(6), NOW(6), 'Kurunegala', 'Colombo'),
    (NOW(6), NOW(6), 'Colombo', 'Kandy'),
    (NOW(6), NOW(6), 'Colombo', 'Galle'),
    (NOW(6), NOW(6), 'Kandy', 'Jaffna');

INSERT INTO buses
    (created_at, updated_at, registration_no, capacity, bus_type,
     availability_status, status)
VALUES
    (NOW(6), NOW(6), 'NW-NB-1234', 20, 'AC', 'AVAILABLE', 'ACTIVE'),
    (NOW(6), NOW(6), 'WP-NB-5678', 30, 'LUXURY', 'AVAILABLE', 'ACTIVE'),
    (NOW(6), NOW(6), 'CP-NC-9012', 40, 'STANDARD', 'AVAILABLE', 'ACTIVE');

-- Generate the correct number of seats for each sample bus.
INSERT INTO seats
    (created_at, updated_at, bus_id, seat_number, seat_type)
WITH RECURSIVE seat_numbers (seat_number) AS (
    SELECT 1
    UNION ALL
    SELECT seat_number + 1 FROM seat_numbers WHERE seat_number < 40
)
SELECT NOW(6), NOW(6), b.bus_id, n.seat_number,
       CASE
           WHEN n.seat_number IN (1, 2) THEN 'PRIORITY'
           WHEN MOD(n.seat_number, 4) IN (0, 1) THEN 'WINDOW'
           ELSE 'AISLE'
       END
FROM buses b
JOIN seat_numbers n ON n.seat_number <= b.capacity
WHERE b.registration_no IN ('NW-NB-1234', 'WP-NB-5678', 'CP-NC-9012');

INSERT INTO drivers
    (created_at, updated_at, first_name, last_name, license_no, phone,
     availability_status)
VALUES
    (NOW(6), NOW(6), 'Nimal', 'Perera', 'B1234567', '0713456789', 'AVAILABLE'),
    (NOW(6), NOW(6), 'Saman', 'Kumara', 'B7654321', '0723456789', 'AVAILABLE'),
    (NOW(6), NOW(6), 'Ravi', 'Fernando', 'B2468101', '0753456789', 'AVAILABLE');

-- =========================================================
-- 3. FUTURE SCHEDULES
-- =========================================================

INSERT INTO schedules
    (created_at, updated_at, route_id, bus_id, driver_id, travel_date,
     departure_time, arrival_time, fare, status)
SELECT NOW(6), NOW(6), r.route_id, b.bus_id, d.driver_id,
       DATE_ADD(CURDATE(), INTERVAL 1 DAY), '06:30:00', '09:30:00',
       750.00, 'SCHEDULED'
FROM routes r, buses b, drivers d
WHERE r.origin = 'Kurunegala' AND r.destination = 'Colombo'
  AND b.registration_no = 'NW-NB-1234'
  AND d.license_no = 'B1234567';

INSERT INTO schedules
    (created_at, updated_at, route_id, bus_id, driver_id, travel_date,
     departure_time, arrival_time, fare, status)
SELECT NOW(6), NOW(6), r.route_id, b.bus_id, d.driver_id,
       DATE_ADD(CURDATE(), INTERVAL 2 DAY), '07:00:00', '10:30:00',
       1100.00, 'SCHEDULED'
FROM routes r, buses b, drivers d
WHERE r.origin = 'Colombo' AND r.destination = 'Kandy'
  AND b.registration_no = 'WP-NB-5678'
  AND d.license_no = 'B7654321';

INSERT INTO schedules
    (created_at, updated_at, route_id, bus_id, driver_id, travel_date,
     departure_time, arrival_time, fare, status)
SELECT NOW(6), NOW(6), r.route_id, b.bus_id, d.driver_id,
       DATE_ADD(CURDATE(), INTERVAL 3 DAY), '08:00:00', '11:00:00',
       900.00, 'DELAYED'
FROM routes r, buses b, drivers d
WHERE r.origin = 'Colombo' AND r.destination = 'Galle'
  AND b.registration_no = 'CP-NC-9012'
  AND d.license_no = 'B2468101';

INSERT INTO schedules
    (created_at, updated_at, route_id, bus_id, driver_id, travel_date,
     departure_time, arrival_time, fare, status)
SELECT NOW(6), NOW(6), r.route_id, b.bus_id, d.driver_id,
       DATE_SUB(CURDATE(), INTERVAL 5 DAY), '06:00:00', '09:00:00',
       750.00, 'COMPLETED'
FROM routes r, buses b, drivers d
WHERE r.origin = 'Kurunegala' AND r.destination = 'Colombo'
  AND b.registration_no = 'NW-NB-1234'
  AND d.license_no = 'B1234567';

-- =========================================================
-- 4. BOOKINGS AND RESERVED SEATS
-- =========================================================

-- Confirmed future booking with two seats.
INSERT INTO bookings
    (created_at, updated_at, passenger_id, schedule_id, booking_date,
     booking_status, passenger_count, total_amount, booking_reference)
SELECT NOW(6), NOW(6), p.passenger_id, s.schedule_id, NOW(6),
       'CONFIRMED', 2, s.fare * 2, 'SRL-DEMO-0001'
FROM passengers p
JOIN users u ON u.user_id = p.user_id
JOIN schedules s ON s.travel_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
JOIN routes r ON r.route_id = s.route_id
WHERE u.email = 'passenger1@smartroute.lk'
  AND r.origin = 'Kurunegala' AND r.destination = 'Colombo';

INSERT INTO booking_seats
    (created_at, updated_at, booking_id, seat_id, schedule_id)
SELECT NOW(6), NOW(6), bk.booking_id, st.seat_id, bk.schedule_id
FROM bookings bk
JOIN schedules sc ON sc.schedule_id = bk.schedule_id
JOIN seats st ON st.bus_id = sc.bus_id AND st.seat_number IN (1, 2)
WHERE bk.booking_reference = 'SRL-DEMO-0001';

-- Pending-payment future booking.
INSERT INTO bookings
    (created_at, updated_at, passenger_id, schedule_id, booking_date,
     booking_status, passenger_count, total_amount, booking_reference)
SELECT NOW(6), NOW(6), p.passenger_id, s.schedule_id, NOW(6),
       'PENDING_PAYMENT', 1, s.fare, 'SRL-DEMO-0002'
FROM passengers p
JOIN users u ON u.user_id = p.user_id
JOIN schedules s ON s.travel_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
JOIN routes r ON r.route_id = s.route_id
WHERE u.email = 'passenger2@smartroute.lk'
  AND r.origin = 'Colombo' AND r.destination = 'Kandy';

INSERT INTO booking_seats
    (created_at, updated_at, booking_id, seat_id, schedule_id)
SELECT NOW(6), NOW(6), bk.booking_id, st.seat_id, bk.schedule_id
FROM bookings bk
JOIN schedules sc ON sc.schedule_id = bk.schedule_id
JOIN seats st ON st.bus_id = sc.bus_id AND st.seat_number = 3
WHERE bk.booking_reference = 'SRL-DEMO-0002';

-- Completed historical booking for review/report testing.
INSERT INTO bookings
    (created_at, updated_at, passenger_id, schedule_id, booking_date,
     booking_status, passenger_count, total_amount, booking_reference)
SELECT DATE_SUB(NOW(6), INTERVAL 7 DAY), NOW(6), p.passenger_id,
       s.schedule_id, DATE_SUB(NOW(6), INTERVAL 7 DAY),
       'COMPLETED', 1, s.fare, 'SRL-DEMO-0003'
FROM passengers p
JOIN users u ON u.user_id = p.user_id
JOIN schedules s ON s.travel_date = DATE_SUB(CURDATE(), INTERVAL 5 DAY)
JOIN routes r ON r.route_id = s.route_id
WHERE u.email = 'passenger1@smartroute.lk'
  AND r.origin = 'Kurunegala' AND r.destination = 'Colombo';

INSERT INTO booking_seats
    (created_at, updated_at, booking_id, seat_id, schedule_id)
SELECT NOW(6), NOW(6), bk.booking_id, st.seat_id, bk.schedule_id
FROM bookings bk
JOIN schedules sc ON sc.schedule_id = bk.schedule_id
JOIN seats st ON st.bus_id = sc.bus_id AND st.seat_number = 4
WHERE bk.booking_reference = 'SRL-DEMO-0003';

-- =========================================================
-- 5. PAYMENTS, REVIEW, SUPPORT AND NOTIFICATIONS
-- =========================================================

INSERT INTO payments
    (created_at, updated_at, booking_id, payment_method,
     transaction_reference, payment_date, amount, payment_status)
SELECT NOW(6), NOW(6), booking_id, 'CARD', 'TXN-DEMO-0001',
       NOW(6), total_amount, 'SUCCESS'
FROM bookings WHERE booking_reference = 'SRL-DEMO-0001';

INSERT INTO payments
    (created_at, updated_at, booking_id, payment_method,
     transaction_reference, payment_date, amount, payment_status)
SELECT NOW(6), NOW(6), booking_id, 'CARD', 'TXN-DEMO-0002',
       NOW(6), total_amount, 'FAILED'
FROM bookings WHERE booking_reference = 'SRL-DEMO-0002';

INSERT INTO payments
    (created_at, updated_at, booking_id, payment_method,
     transaction_reference, payment_date, amount, payment_status)
SELECT DATE_SUB(NOW(6), INTERVAL 7 DAY), NOW(6), booking_id,
       'BANK_TRANSFER', 'TXN-DEMO-0003', DATE_SUB(NOW(6), INTERVAL 7 DAY),
       total_amount, 'SUCCESS'
FROM bookings WHERE booking_reference = 'SRL-DEMO-0003';

INSERT INTO reviews
    (created_at, updated_at, passenger_id, booking_id, rating, comment, review_date)
SELECT NOW(6), NOW(6), passenger_id, booking_id, 5,
       'Comfortable journey and friendly service.', NOW(6)
FROM bookings WHERE booking_reference = 'SRL-DEMO-0003';

INSERT INTO support_tickets
    (created_at, updated_at, user_id, handled_by_staff_id,
     subject, description, status, created_date)
SELECT NOW(6), NOW(6), passenger_user.user_id, customer_staff.staff_id,
       'Payment confirmation delay',
       'My payment was completed but confirmation was delayed.',
       'IN_PROGRESS', NOW(6)
FROM users passenger_user
JOIN users staff_user ON staff_user.email = 'customer.service@smartroute.lk'
JOIN staff customer_staff ON customer_staff.user_id = staff_user.user_id
WHERE passenger_user.email = 'passenger1@smartroute.lk';

INSERT INTO support_tickets
    (created_at, updated_at, user_id, handled_by_staff_id,
     subject, description, status, created_date)
SELECT NOW(6), NOW(6), user_id, NULL,
       'Change travel date',
       'I need assistance changing my selected travel date.',
       'OPEN', NOW(6)
FROM users WHERE email = 'passenger2@smartroute.lk';

INSERT INTO notifications
    (created_at, updated_at, user_id, notification_type,
     message, sent_date_time, status)
SELECT NOW(6), NOW(6), user_id, 'BOOKING_CONFIRMATION',
       'Booking SRL-DEMO-0001 has been confirmed.', NOW(6), 'SENT'
FROM users WHERE email = 'passenger1@smartroute.lk';

INSERT INTO notifications
    (created_at, updated_at, user_id, notification_type,
     message, sent_date_time, status)
SELECT NOW(6), NOW(6), user_id, 'PAYMENT_CONFIRMATION',
       'Payment TXN-DEMO-0001 was successful.', NOW(6), 'READ'
FROM users WHERE email = 'passenger1@smartroute.lk';

INSERT INTO notifications
    (created_at, updated_at, user_id, notification_type,
     message, sent_date_time, status)
SELECT NOW(6), NOW(6), user_id, 'SCHEDULE_DELAY',
       'The Colombo to Galle schedule has been delayed.', NOW(6), 'CREATED'
FROM users WHERE user_type = 'PASSENGER';

-- =========================================================
-- DEMO LOGIN ACCOUNTS (all use Password@123)
-- =========================================================
-- passenger1@smartroute.lk        -> PASSENGER
-- passenger2@smartroute.lk        -> PASSENGER
-- customer.service@smartroute.lk  -> CUSTOMER_SERVICE
-- coordinator@smartroute.lk       -> TRANSPORT_COORDINATOR
-- finance@smartroute.lk           -> FINANCE_OFFICER
-- operations@smartroute.lk        -> OPERATIONS_MANAGER
-- admin@smartroute.lk             -> SYSTEM_ADMIN
-- itsupport@smartroute.lk         -> IT_SUPPORT

