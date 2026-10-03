package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.AuthResponse;
import lk.smartroute.dto.ApiDtos.LoginRequest;
import lk.smartroute.dto.ApiDtos.RegisterResponse;
import lk.smartroute.dto.ApiDtos.RegisterRequest;
import lk.smartroute.dto.ApiDtos.RegistrationResendRequest;
import lk.smartroute.dto.ApiDtos.RegistrationVerifyRequest;
import lk.smartroute.service.AuthService;
import lk.smartroute.service.PasswordResetService;
import lk.smartroute.dto.WalletDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/register/verify")
    public MessageResponse verifyRegistration(@Valid @RequestBody RegistrationVerifyRequest request) {
        return authService.verifyRegistration(request);
    }

    @PostMapping("/register/resend")
    public RegisterResponse resendRegistration(@Valid @RequestBody RegistrationResendRequest request) {
        return authService.resendRegistrationOtp(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    @PostMapping("/forgot-password/request")
    public MessageResponse forgotRequest(@Valid @RequestBody ForgotPasswordRequest request) { return passwordResetService.request(request); }

    @PostMapping("/forgot-password/verify")
    public MessageResponse forgotVerify(@Valid @RequestBody ForgotPasswordVerifyRequest request) { return passwordResetService.verify(request); }

    @PostMapping("/forgot-password/reset")
    public MessageResponse forgotReset(@Valid @RequestBody ResetPasswordRequest request) { return passwordResetService.reset(request); }
}
