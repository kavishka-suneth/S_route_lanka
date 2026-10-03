package lk.smartroute.service;
import lk.smartroute.model.Booking;
import lk.smartroute.model.OtpPurpose;
public interface EmailService {
    void sendOtp(String email,String otp,OtpPurpose purpose,int expiryMinutes);

    default void sendTicket(String email, Booking booking, String qrCodeDataUri) {
    }

    default void sendNotificationEmail(String email, String subject, String htmlBody) {
    }
}
