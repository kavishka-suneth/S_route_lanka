# SmartRoute Wallet, Email OTP, and Forgot Password Setup

## Important simulation warning

SmartRoute Wallet and saved-card processing in this project are **simulation features for coursework/demo use only**. They are not PCI-compliant and must not be used for real payments. Users should only use the documented demo card numbers and must not enter real card information.

## Database migration

The wallet/card/OTP schema is created by Flyway migration:

- `V6__wallet_cards_otp.sql`

It creates `wallets`, `customer_cards`, `otp_verifications`, and `wallet_transactions`. Existing passenger users are backfilled with a zero-balance wallet. New passenger registration also creates a wallet automatically.

Email verification is added by:

- `V11__add_email_verification_to_users.sql`

It adds `users.email_verified`, marks existing users as verified so current logins continue working, and lets new passenger registrations start as unverified until their email OTP is confirmed.

Start the application normally with Flyway enabled. Do not edit or renumber already-applied migrations.

## Required/optional environment variables

Copy `.env.example` to `.env` locally. The real `.env` must never be committed or included in deployment archives.

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-address@gmail.com
MAIL_PASSWORD=your-google-app-password
MAIL_FROM=your-address@gmail.com
MAIL_DEVELOPMENT_FALLBACK=true
```

Other existing variables such as `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` remain unchanged.

Optional OTP/wallet limits:

```properties
OTP_EXPIRY_MINUTES=5
OTP_RESEND_COOLDOWN_SECONDS=60
OTP_MAX_ATTEMPTS=5
WALLET_MIN_TOP_UP=100.00
WALLET_MAX_TOP_UP=50000.00
```

## Gmail App Password setup

1. Use a Google account with 2-Step Verification enabled.
2. Open the Google Account security settings and create an App Password for the local SmartRoute application.
3. Put the generated App Password in `MAIL_PASSWORD`; do not use the normal Google account password.
4. Set `MAIL_USERNAME` and `MAIL_FROM` to the sender Gmail address.
5. Restart Spring Boot after changing environment values.

No Gmail username, password, App Password, API key, or token is hardcoded in the project.

## Development fallback

If SMTP credentials are blank and `MAIL_DEVELOPMENT_FALLBACK=true`, the application records only that an email delivery attempt occurred. It deliberately does **not** print or return the OTP. To exercise the full OTP flow manually, configure a working SMTP account.

## Manual test flows

### Registration email verification

1. Open `/register.html`.
2. Create a passenger with a strong password.
3. Check the registered email for the `REGISTRATION` OTP.
4. Enter the code in the verification dialog.
5. Confirm the UI redirects to `/login.html` and login succeeds only after verification.
6. Attempting to log in before verification must show `Please verify your email before logging in`.

### Add a saved simulated card

1. Register, verify email, and log in as a passenger.
2. Open `/wallet.html`.
3. Choose **Add Card**.
4. Use `4111111111111111` or `5555555555554444`, a future expiry date, and any 3-digit demo CVV.
5. Confirm the card is validated and saved immediately.
6. Confirm the card appears masked as `**** **** **** 1111` or `**** **** **** 4444`.

### Wallet top-up

1. Open `/wallet.html` and choose **Top up wallet**.
2. Select a verified saved card.
3. Choose Rs. 500 / 1,000 / 2,000 / 5,000 or enter a custom amount between Rs. 100 and Rs. 50,000.
4. Enter the new email OTP.
5. Confirm the balance increases once and a `TOP_UP` transaction appears.
6. Retrying the same OTP must fail and must not credit the wallet again.

### Seat booking with wallet

1. Search a schedule and select seats using the existing flow.
2. On checkout, choose **SmartRoute Wallet**.
3. Verify the wallet balance, booking total, and remaining balance preview.
4. If the balance is insufficient, use the Wallet link to top up first.
5. Submit payment. A successful payment creates the existing booking/payment/ticket/receipt records, deducts the exact amount, and records a `BOOKING_PAYMENT` wallet transaction.
6. Repeating the wallet payment for the same booking must not charge it twice.

### Cancellation/refund

1. Open **My bookings** and cancel an eligible confirmed wallet-paid booking.
2. Confirm the booking becomes `CANCELLED`, seats are released, the existing payment becomes `REFUNDED`, and the exact amount returns to the same wallet.
3. Confirm a `REFUND` wallet transaction appears.
4. Repeating cancellation/refund must not add a second refund.

### Forgot password

1. Open `/login.html` and choose **Forgot password?**.
2. Enter the account email. The UI always displays the neutral response regardless of whether the account exists.
3. Enter the emailed 6-digit OTP within 5 minutes.
4. Enter a new password containing upper/lowercase letters, a number, and a special character.
5. Return to login and sign in with the new password. The reset flow does not automatically log the user in.

## Security notes

- OTPs use `SecureRandom` and are stored only as BCrypt hashes.
- OTP purposes are `REGISTRATION`, `CARD_VERIFICATION`, `WALLET_TOP_UP`, and `PASSWORD_RESET`.
- OTPs are tied to email/user, purpose, and pending operation.
- Five-attempt maximum, five-minute expiry, 60-second resend cooldown, and single-use consumption are enforced server-side.
- Wallet/card endpoints require the authenticated `PASSENGER` role.
- Card ownership, wallet ownership, booking ownership, payment state, and refund state are checked server-side.
- `BigDecimal` is used for monetary values.
- Pessimistic row locking protects wallet balance updates and booking payment state from concurrent double-spending.
