package lk.smartroute.service;
import lk.smartroute.dto.WalletDtos.*;
public interface PasswordResetService{
 MessageResponse request(ForgotPasswordRequest r);
 MessageResponse verify(ForgotPasswordVerifyRequest r);
 MessageResponse reset(ResetPasswordRequest r);
}
