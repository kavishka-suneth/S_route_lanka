package lk.smartroute.service.impl;
import lk.smartroute.model.*;import lk.smartroute.repository.*;import lk.smartroute.service.*;
import org.springframework.beans.factory.annotation.Value;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.transaction.annotation.Propagation;
import java.security.SecureRandom;import java.time.*;
@Service @Transactional
public class OtpServiceImpl implements OtpService{
 private final OtpVerificationRepository repo;private final PasswordEncoder encoder;private final EmailService emailService;private final SecureRandom random=new SecureRandom();private final int expiry,cooldown,maxAttempts;
 public OtpServiceImpl(OtpVerificationRepository repo,PasswordEncoder encoder,EmailService emailService,@Value("${app.otp.expiry-minutes:5}")int expiry,@Value("${app.otp.resend-cooldown-seconds:60}")int cooldown,@Value("${app.otp.max-attempts:5}")int maxAttempts){this.repo=repo;this.encoder=encoder;this.emailService=emailService;this.expiry=expiry;this.cooldown=cooldown;this.maxAttempts=maxAttempts;}
 @Transactional(noRollbackFor = RuntimeException.class)
 public OtpVerification issue(User user,String email,OtpPurpose purpose,String key,String payload,boolean neutral){
  var old=repo.findFirstByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(email,purpose);
  if(old.isPresent()&&old.get().getLastSentAt().plusSeconds(cooldown).isAfter(LocalDateTime.now())){if(neutral)return old.get();throw new IllegalStateException("Please wait before requesting another verification code");}
  repo.invalidateAll(email,purpose);String code=String.format("%06d",random.nextInt(1_000_000));LocalDateTime now=LocalDateTime.now();var o=new OtpVerification();o.setUser(user);o.setEmail(email.toLowerCase());o.setPurpose(purpose);o.setOperationKey(key);o.setLegacyOperationId(key);o.setLegacyResendAvailableAt(now.plusSeconds(cooldown));o.setOtpHash(encoder.encode(code));o.setPayloadJson(payload);o.setExpiresAt(now.plusMinutes(expiry));o.setLastSentAt(now);o.setAttempts(0);o.setUsed(false);o.setVerified(false);var saved=repo.save(o);emailService.sendOtp(email,code,purpose,expiry);return saved;
 }
 @Transactional(propagation=Propagation.REQUIRES_NEW, noRollbackFor=IllegalArgumentException.class)
 public OtpVerification verify(String email,OtpPurpose purpose,String key,String code){return verifyInternal(email,purpose,key,code,false);}
 @Transactional(propagation=Propagation.REQUIRES_NEW, noRollbackFor=IllegalArgumentException.class)
 public OtpVerification verifyAndConsume(String email,OtpPurpose purpose,String key,String code){return verifyInternal(email,purpose,key,code,true);}
 private OtpVerification verifyInternal(String email,OtpPurpose purpose,String key,String code,boolean consume){var list=repo.findActiveForUpdate(email,purpose,key);if(list.isEmpty())throw new IllegalArgumentException("Invalid or expired verification code");var o=list.get(0);if(o.getExpiresAt().isBefore(LocalDateTime.now())){o.setUsed(true);throw new IllegalArgumentException("Verification code has expired");}if(o.getAttempts()>=maxAttempts){o.setUsed(true);throw new IllegalArgumentException("Maximum verification attempts exceeded");}if(!encoder.matches(code,o.getOtpHash())){o.setAttempts(o.getAttempts()+1);if(o.getAttempts()>=maxAttempts)o.setUsed(true);throw new IllegalArgumentException("Invalid verification code");}o.setVerified(true);if(consume)o.setUsed(true);return o;}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public OtpVerification consumeVerified(String email,OtpPurpose p,String key){var list=repo.findActiveForUpdate(email,p,key);if(list.isEmpty())throw new IllegalArgumentException("Verification is required or has expired");var o=list.get(0);if(!o.isVerified()||o.getExpiresAt().isBefore(LocalDateTime.now()))throw new IllegalArgumentException("Verification is required or has expired");o.setUsed(true);return o;}
 public OtpVerification verified(String email,OtpPurpose p,String key){var o=repo.findFirstByEmailIgnoreCaseAndPurposeAndOperationKeyAndUsedFalseOrderByCreatedAtDesc(email,p,key).orElseThrow(()->new IllegalArgumentException("Verification is required"));if(!o.isVerified()||o.getExpiresAt().isBefore(LocalDateTime.now()))throw new IllegalArgumentException("Verification is required or has expired");return o;}
 public void consume(OtpVerification o){o.setUsed(true);repo.save(o);}
 public void invalidateAll(String email,OtpPurpose p){repo.invalidateAll(email,p);}
}
