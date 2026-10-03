package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.dto.WalletDtos.MessageResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    MessageResponse verifyRegistration(RegistrationVerifyRequest request);

    RegisterResponse resendRegistrationOtp(RegistrationResendRequest request);
}
