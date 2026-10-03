# SmartRoute Lanka API Endpoints

Base URL: `http://localhost:8080/api`

Protected endpoints require `Authorization: Bearer <token>`.

## Authentication

Public endpoints:

- `POST /auth/register`
- `POST /auth/register/verify`
- `POST /auth/register/resend`
- `POST /auth/login`
- `POST /auth/forgot-password/request`
- `POST /auth/forgot-password/verify`
- `POST /auth/forgot-password/reset`

Registration creates a passenger account with `emailVerified=false`, creates the passenger profile and zero-balance wallet, sends a `REGISTRATION` OTP, and does not return a JWT.

Register:

```json
{
  "firstName": "Test",
  "lastName": "Passenger",
  "email": "passenger@example.com",
  "password": "Passenger123!",
  "phone": "0771234567"
}
```

Verify registration:

```json
{
  "email": "passenger@example.com",
  "otp": "123456"
}
```

Resend registration OTP:

```json
{
  "email": "passenger@example.com"
}
```

Login:

```json
{
  "email": "passenger@example.com",
  "password": "Passenger123!"
}
```

Forgot password request always returns the neutral response:

`If an account exists for this email, a verification code has been sent.`

Forgot password verify:

```json
{
  "email": "passenger@example.com",
  "otp": "123456"
}
```

Reset password:

```json
{
  "email": "passenger@example.com",
  "newPassword": "NewPass123!",
  "confirmPassword": "NewPass123!"
}
```

## Users

- `GET /users/me`
- `PUT /users/me`
- `PATCH /users/me/password`
- `GET /users?role=PASSENGER&active=true&page=0&size=20`
- `GET /users/{id}`
- `PATCH /users/{id}/role`
- `PATCH /users/{id}/active`

## Routes

- `GET /routes`
- `GET /routes/{id}`
- `POST /routes`
- `PUT /routes/{id}`
- `DELETE /routes/{id}`

## Buses

- `GET /buses?active=true&availabilityStatus=AVAILABLE`
- `GET /buses/available?travelDate=2026-10-01&departureTime=08:00&arrivalTime=12:00&excludeScheduleId=1`
- `GET /buses/{id}`
- `POST /buses`
- `PUT /buses/{id}`
- `DELETE /buses/{id}`

`availabilityStatus` is an exact operational filter. Admins may use `AVAILABLE`, `UNAVAILABLE`, or `MAINTENANCE`; `ASSIGNED` is legacy-only and cannot be selected manually.

`GET /buses/available` returns active buses with operational availability `AVAILABLE` and no overlapping `SCHEDULED` or `DELAYED` schedule in the requested time range. It returns HTTP `400` for missing/invalid time ranges.

## Drivers

- `GET /drivers?availabilityStatus=AVAILABLE`
- `GET /drivers/available?travelDate=2026-10-01&departureTime=08:00&arrivalTime=12:00&excludeScheduleId=1`
- `GET /drivers/{id}`
- `POST /drivers`
- `PUT /drivers/{id}`
- `DELETE /drivers/{id}`

Driver availability filters are exact. Use `availabilityStatus=UNAVAILABLE` to see only unavailable drivers and `availabilityStatus=MAINTENANCE` to see maintenance records. Optional phone numbers must be Sri Lankan mobile numbers such as `0711234567` or `+94711234567`.

`GET /drivers/available` returns operationally available drivers with no overlapping `SCHEDULED` or `DELAYED` schedule. `excludeScheduleId` is used while editing an existing schedule.

## Schedules

- `GET /schedules/search?origin=Colombo&destination=Kandy&date=2026-10-01`
- `GET /schedules/upcoming`
- `GET /schedules`
- `GET /schedules/{id}`
- `POST /schedules`
- `PUT /schedules/{id}`
- `PATCH /schedules/{id}/status`
- `DELETE /schedules/{id}`

Create/update schedule payload:

```json
{
  "routeId": 1,
  "busId": 1,
  "driverId": 1,
  "travelDate": "2026-10-01",
  "departureTime": "08:00:00",
  "arrivalTime": "12:00:00",
  "fare": 950.00,
  "status": "SCHEDULED"
}
```

Schedule creation and updates verify that the route is active, the bus is active and operationally available, the driver is operationally available, departure is in the future for active schedules, arrival is after departure, and neither resource has an overlapping active assignment. Overlap means `newDeparture < existingArrival` and `newArrival > existingDeparture`; touching endpoints are allowed.

HTTP `409 Conflict` responses include:

- `Bus is already assigned to an overlapping schedule`
- `Driver is already assigned to an overlapping schedule`
- `Bus is unavailable or under maintenance`
- `Driver is unavailable or under maintenance`
- `Cannot assign bus with capacity below existing reserved seat number <n>`

Changing status to `CANCELLED` releases seats, sends existing cancellation notifications, and refunds confirmed bookings through the existing payment/refund flow. Changing status to `COMPLETED` does not refund payments. `CANCELLED` and `COMPLETED` schedules do not block future bus/driver assignments.

## Bookings

- `GET /bookings/available-seats/{scheduleId}`
- `POST /bookings`
- `GET /bookings/{id}`
- `GET /bookings/my`
- `PUT /bookings/{id}/seats`
- `PATCH /bookings/{id}/cancel`

## Payments and Receipts

- `POST /payments/bookings/{bookingId}`
- `GET /payments/{paymentId}/verify`
- `POST /payments/{paymentId}/refund`
- `GET /payments/bookings/{bookingId}/receipt`

Use `/wallet/bookings/{bookingId}/pay` for SmartRoute Wallet payments.

## SmartRoute Wallet

Passenger JWT required. This is a simulated wallet and card system for demo/coursework use only. Do not enter real card information.

- `GET /wallet` - current balance
- `GET /wallet/transactions?page=0&size=20` - wallet history
- `GET /wallet/cards` - owned saved cards, masked only
- `POST /wallet/cards` - validate and save a simulated card without storing full card number or CVV
- `POST /wallet/cards/otp` - legacy optional card verification OTP flow
- `POST /wallet/cards/verify` - legacy optional OTP confirmation and save
- `DELETE /wallet/cards/{cardId}` - remove an owned saved card
- `POST /wallet/topups/otp` - request a wallet top-up OTP
- `POST /wallet/topups/confirm` - confirm top-up and credit wallet once
- `POST /wallet/bookings/{bookingId}/pay` - atomically pay a pending booking with wallet balance

Add saved simulated card:

```json
  {
    "cardholderName": "Demo Passenger",
    "brand": "VISA",
    "cardNumber": "4111111111111111",
    "expiryMonth": 12,
    "expiryYear": 2030,
  "cvv": "123"
}
```

Verify top-up OTP:

```json
{
  "operationKey": "CARD-or-TOPUP-key",
  "otp": "123456"
}
```

Top-up request:

```json
{
  "cardId": 1,
  "amount": 2000.00
}
```

Wallet transaction types are `TOP_UP`, `BOOKING_PAYMENT`, and `REFUND`. Statuses are `PENDING`, `SUCCESS`, `FAILED`, and `REFUNDED`. Booking wallet payment creates the existing payment record using `SMARTROUTE_WALLET`.

Demo card numbers:

- `4111111111111111` - accepted Visa simulation
- `5555555555554444` - accepted Mastercard simulation
- `4000000000000002` - simulated decline

## Reviews

- `POST /reviews`
- `GET /reviews`
- `GET /reviews/my`

## Support Tickets

- `POST /support-tickets`
- `GET /support-tickets/my`
- `GET /support-tickets?status=OPEN`
- `PATCH /support-tickets/{id}/response`
- `PATCH /support-tickets/{id}/status`

## Notifications

- `GET /notifications/my`
- `GET /notifications/unread-count`
- `PATCH /notifications/{id}/read`
- `GET /notifications?status=SENT`

## Reports

- `GET /reports/revenue/total`
- `GET /reports/revenue?from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z`
- `GET /reports/bookings/count`
- `GET /reports/ticket-sales`
- `GET /reports/schedule-occupancy`
- `GET /reports/cancelled-bookings`
- `GET /reports/users`
- `GET /reports/payments`
- `GET /reports/export/summary.csv`
