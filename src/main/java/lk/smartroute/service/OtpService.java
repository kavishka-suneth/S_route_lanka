package lk.smartroute.service;
import lk.smartroute.model.*;
public interface OtpService {
 OtpVerification issue(User user,String email,OtpPurpose purpose,String operationKey,String payload,boolean neutralCooldown);
 OtpVerification verify(String email,OtpPurpose purpose,String operationKey,String otp);
 OtpVerification verifyAndConsume(String email,OtpPurpose purpose,String operationKey,String otp);
 OtpVerification consumeVerified(String email,OtpPurpose purpose,String operationKey);
 OtpVerification verified(String email,OtpPurpose purpose,String operationKey);
 void consume(OtpVerification otp);
 void invalidateAll(String email,OtpPurpose purpose);
}
