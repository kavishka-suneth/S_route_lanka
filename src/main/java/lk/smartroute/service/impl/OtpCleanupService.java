package lk.smartroute.service.impl;

import lk.smartroute.repository.OtpVerificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class OtpCleanupService {
    private final OtpVerificationRepository otps;
    public OtpCleanupService(OtpVerificationRepository otps) { this.otps = otps; }

    @Scheduled(fixedDelayString = "${app.otp.cleanup-ms:3600000}")
    @Transactional
    public void cleanupExpiredOtps() {
        otps.deleteByExpiresAtBefore(LocalDateTime.now().minusHours(24));
    }
}
