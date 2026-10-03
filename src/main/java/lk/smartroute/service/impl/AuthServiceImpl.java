package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.AuthResponse;
import lk.smartroute.dto.ApiDtos.LoginRequest;
import lk.smartroute.dto.ApiDtos.RegisterResponse;
import lk.smartroute.dto.ApiDtos.RegisterRequest;
import lk.smartroute.dto.ApiDtos.RegistrationResendRequest;
import lk.smartroute.dto.ApiDtos.RegistrationVerifyRequest;
import lk.smartroute.dto.WalletDtos.MessageResponse;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.exception.ConflictException;
import lk.smartroute.model.Passenger;
import lk.smartroute.model.OtpPurpose;
import lk.smartroute.model.OtpVerification;
import lk.smartroute.model.User;
import lk.smartroute.model.UserType;
import lk.smartroute.repository.PassengerRepository;
import lk.smartroute.repository.UserRepository;
import lk.smartroute.repository.WalletRepository;
import lk.smartroute.model.Wallet;
import java.math.BigDecimal;
import lk.smartroute.security.JwtService;
import lk.smartroute.service.AuthService;
import lk.smartroute.service.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final PassengerRepository passengers;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WalletRepository wallets;
    private final OtpService otpService;
    private final long expirationMs;
    private static final String REGISTRATION_OPERATION = "REGISTRATION";

    public AuthServiceImpl(
        UserRepository users,
        PassengerRepository passengers,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        WalletRepository wallets,
        OtpService otpService,
        @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.users = users;
        this.passengers = passengers;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.wallets = wallets;
        this.otpService = otpService;
        this.expirationMs = expirationMs;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (!strongPassword(request.password())) {
            throw new IllegalArgumentException("Password must be 8-100 characters and contain upper case, lower case, a number, and a special character");
        }
        var existing = users.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.isEmailVerified()) {
                throw new ConflictException("Email already registered");
            }
            if (user.getUserType() != UserType.PASSENGER) {
                throw new ConflictException("Email already registered");
            }
            ensurePassengerProfile(user);
            ensureWallet(user);
            LocalDateTime expiresAt = issueRegistrationOtp(user).getExpiresAt();
            return new RegisterResponse("This email is already pending verification. A new verification code has been sent.", email, expiresAt, DtoMapper.user(user));
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUserType(UserType.PASSENGER);
        user.setEmailVerified(false);
        User saved = users.save(user);
        ensurePassengerProfile(saved);
        ensureWallet(saved);
        LocalDateTime expiresAt = issueRegistrationOtp(saved).getExpiresAt();
        return new RegisterResponse("Registration created. Verify your email before logging in.", email, expiresAt, DtoMapper.user(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!user.isActive()) {
            throw new IllegalArgumentException("This account is inactive");
        }
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Please verify your email before logging in");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return response(user);
    }

    @Override
    public MessageResponse verifyRegistration(RegistrationVerifyRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));
        if (user.isEmailVerified()) {
            otpService.invalidateAll(email, OtpPurpose.REGISTRATION);
            return new MessageResponse("Email is already verified. Please log in.");
        }
        otpService.verifyAndConsume(email, OtpPurpose.REGISTRATION, REGISTRATION_OPERATION, request.otp());
        user.setEmailVerified(true);
        otpService.invalidateAll(email, OtpPurpose.REGISTRATION);
        return new MessageResponse("Email verified successfully. Please log in.");
    }

    @Override
    public RegisterResponse resendRegistrationOtp(RegistrationResendRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException("Registration verification is not available for this email"));
        if (user.isEmailVerified()) {
            throw new ConflictException("Email is already verified");
        }
        LocalDateTime expiresAt = issueRegistrationOtp(user).getExpiresAt();
        return new RegisterResponse("Verification code sent. Check your email.", email, expiresAt, DtoMapper.user(user));
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.generate(user), "Bearer", expirationMs, DtoMapper.user(user));
    }

    private OtpVerification issueRegistrationOtp(User user) {
        return otpService.issue(user, user.getEmail(), OtpPurpose.REGISTRATION, REGISTRATION_OPERATION, null, false);
    }

    private void ensurePassengerProfile(User user) {
        if (user.getPassenger() != null) {
            return;
        }
        Passenger passenger = new Passenger();
        passenger.setUser(user);
        Passenger savedPassenger = passengers.save(passenger);
        user.setPassenger(savedPassenger);
    }

    private void ensureWallet(User user) {
        if (wallets.findByUserId(user.getId()).isPresent()) {
            return;
        }
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallets.save(wallet);
    }

    private boolean strongPassword(String password) {
        return password != null
            && password.length() >= 8
            && password.length() <= 100
            && password.matches(".*[A-Z].*")
            && password.matches(".*[a-z].*")
            && password.matches(".*\\d.*")
            && password.matches(".*[^A-Za-z0-9].*");
    }
}
