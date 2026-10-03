package lk.smartroute.service.impl;
import lk.smartroute.dto.WalletDtos.*;import lk.smartroute.model.*;import lk.smartroute.repository.UserRepository;import lk.smartroute.service.*;
import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class PasswordResetServiceImpl implements PasswordResetService{
 private static final String KEY="PASSWORD_RESET";private static final String NEUTRAL="If an account exists for this email, a verification code has been sent.";
 private final UserRepository users;private final OtpService otps;private final PasswordEncoder encoder;
 public PasswordResetServiceImpl(UserRepository users,OtpService otps,PasswordEncoder encoder){this.users=users;this.otps=otps;this.encoder=encoder;}
 @Transactional(noRollbackFor = RuntimeException.class)
 public MessageResponse request(ForgotPasswordRequest r){String email=r.email().trim().toLowerCase();users.findByEmailIgnoreCase(email).filter(User::isActive).ifPresent(u->{try{otps.issue(u,email,OtpPurpose.PASSWORD_RESET,KEY,null,true);}catch(RuntimeException ignored){}});return new MessageResponse(NEUTRAL);}
 public MessageResponse verify(ForgotPasswordVerifyRequest r){otps.verify(r.email().trim().toLowerCase(),OtpPurpose.PASSWORD_RESET,KEY,r.otp());return new MessageResponse("Verification successful. You may now set a new password.");}
 public MessageResponse reset(ResetPasswordRequest r){if(!r.newPassword().equals(r.confirmPassword()))throw new IllegalArgumentException("Passwords do not match");if(!strong(r.newPassword()))throw new IllegalArgumentException("Password must be 8-100 characters and contain upper case, lower case, a number, and a special character");String email=r.email().trim().toLowerCase();var u=users.findByEmailIgnoreCase(email).orElseThrow(()->new IllegalArgumentException("Password reset session is invalid or expired"));otps.consumeVerified(email,OtpPurpose.PASSWORD_RESET,KEY);u.setPasswordHash(encoder.encode(r.newPassword()));otps.invalidateAll(email,OtpPurpose.PASSWORD_RESET);return new MessageResponse("Password reset successful. Please log in with your new password.");}
 private boolean strong(String p){return p!=null&&p.length()>=8&&p.length()<=100&&p.matches(".*[A-Z].*")&&p.matches(".*[a-z].*")&&p.matches(".*\\d.*")&&p.matches(".*[^A-Za-z0-9].*");}
}
