# SmartRoute Lanka Backend

Spring Boot 3 / Java 17 backend with static HTML, CSS, and JavaScript screens for bus scheduling, booking, payments, wallet simulation, notifications, tickets, support, reporting, and administration.

## Requirements

- Java 17
- Maven 3.9+
- MySQL 8+

## Local Setup

1. Create the database:

```sql
CREATE DATABASE smartroute_lanka_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Copy `.env.example` to `.env` and fill local values. Do not commit `.env`.

3. Start the app:

```bash
mvn spring-boot:run
```

Flyway runs migrations from `src/main/resources/db/migration`.

## Email and Gmail App Passwords

The app uses Spring Boot Mail for OTP delivery.

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-address@gmail.com
MAIL_PASSWORD=your-google-app-password
MAIL_FROM=your-address@gmail.com
MAIL_DEVELOPMENT_FALLBACK=true
```

For Gmail, enable 2-Step Verification on the Google account, create an App Password from Google Account security settings, and use that generated App Password as `MAIL_PASSWORD`. Do not use a normal account password and do not hardcode credentials.

If SMTP credentials are blank and `MAIL_DEVELOPMENT_FALLBACK=true`, the app does not send mail and deliberately does not log or return OTP codes.

## New Flyway Migrations

- `V6__wallet_cards_otp.sql` creates wallets, saved simulated cards, OTP records, wallet transactions, and zero-balance wallets for existing passengers.
- `V7__fix_customer_cards_brand.sql` normalizes existing card brand values, `V14__normalize_customer_cards_card_brand.sql` aligns the persisted column name with the JPA mapping, and `V15`/`V16` tolerate older wallet transaction tables with legacy `user_id` and `reference` columns.
- `V8__fix_otp_verification_email.sql`, `V9__fix_wallet_transaction_reference.sql`, and `V10__add_wallet_transaction_reference.sql` are compatibility no-ops because the current `V6` creates those columns.
- `V11__add_email_verification_to_users.sql` adds `users.email_verified`, backfills existing users as verified, and indexes the field.
- `V17__time_based_resource_assignment.sql` normalizes legacy bus/driver `ASSIGNED` availability values back to `AVAILABLE` and adds schedule assignment indexes.

## Bus and Driver Assignment

Bus and driver assignment is time-based. The resource tables store only operational availability: `AVAILABLE`, `UNAVAILABLE`, or `MAINTENANCE`. The legacy `ASSIGNED` enum value remains for backward compatibility, but admins cannot select it and the backend does not use it to determine assignment.

An active assignment is a schedule with status `SCHEDULED` or `DELAYED`. `CANCELLED` and `COMPLETED` schedules do not block later assignments. Time ranges overlap only when `newDeparture < existingArrival` and `newArrival > existingDeparture`, so schedules that touch at endpoints are allowed.

Staff schedule forms should call:

- `GET /api/buses/available?travelDate=2026-10-01&departureTime=08:00&arrivalTime=12:00`
- `GET /api/drivers/available?travelDate=2026-10-01&departureTime=08:00&arrivalTime=12:00`

When editing a schedule, pass `excludeScheduleId=<id>` so the current schedule does not conflict with itself. Conflict responses use HTTP `409`, including `Bus is already assigned to an overlapping schedule`, `Driver is already assigned to an overlapping schedule`, `Bus is unavailable or under maintenance`, and `Driver is unavailable or under maintenance`.

Before making a bus or driver unavailable, under maintenance, or inactive, cancel or reassign any future `SCHEDULED` or `DELAYED` schedules. The Bus and Driver pages show operational status separately from derived current/upcoming assignment state.

## Tests

Run:

```bash
mvn test
```

The automated test suite covers registration verification, blocked login before verification, OTP expiry/attempt/reuse behavior, wallet creation, card masking and ownership, top-up, insufficient balance, wallet booking payment, duplicate payment protection, refund idempotency, forgot password, login with the new password, unauthorized access checks, and time-based bus/driver assignment conflicts and release behavior.

## Demo Card Numbers

Use only these simulated card numbers. Do not enter real card information.

- `4111111111111111` - accepted Visa simulation
- `5555555555554444` - accepted Mastercard simulation
- `4000000000000002` - simulated decline

## Manual Test Flows

Registration verification:

1. Open `/register.html`.
2. Create a passenger with a strong password.
3. Enter the emailed 6-digit OTP in the verification dialog.
4. Confirm login is blocked before verification and succeeds after verification.

Card verification:

1. Log in as a verified passenger.
2. Open `/wallet.html`.
3. Add `4111111111111111` or `5555555555554444` with a future expiry.
4. Confirm the card is validated and saved immediately.
5. Confirm only masked card details are shown.

Wallet top-up:

1. On `/wallet.html`, choose a verified saved card.
2. Select Rs. 500, 1,000, 2,000, 5,000, or enter a custom amount.
3. Enter the `WALLET_TOP_UP` OTP.
4. Confirm the balance increases once and retrying the OTP fails.

Wallet booking payment:

1. Search a schedule, select seats, and go to checkout.
2. Choose `SmartRoute Wallet`.
3. Confirm balance, total, remaining balance, and insufficient-funds warning.
4. Pay. The booking is confirmed, payment/ticket/receipt are created, and the wallet balance is deducted once.

Refund:

1. Cancel an eligible confirmed wallet-paid booking from My bookings.
2. Confirm seats are released, payment becomes `REFUNDED`, and a single `REFUND` transaction restores the exact amount.
3. Repeat cancellation and confirm no duplicate refund is created.

Forgot password:

1. Open `/forgot-password.html`.
2. Submit an email and confirm the neutral response.
3. Enter the `PASSWORD_RESET` OTP.
4. Set a strong new password.
5. Log in with the new password. The reset flow does not automatically log in.

## Documentation

- `docs/API_ENDPOINTS.md`
- `docs/WALLET_EMAIL_SETUP.md`
- `postman/SmartRoute-Lanka.postman_collection.json`
