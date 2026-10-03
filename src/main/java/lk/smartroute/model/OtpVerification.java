package lk.smartroute.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "otp_verifications",
        indexes = @Index(
                name = "idx_otp_lookup",
                columnList = "email,purpose,operation_key,used"
        )
)
@AttributeOverride(
        name = "id",
        column = @Column(
                name = "otp_id",
                nullable = false,
                updatable = false
        )
)
@Getter
@Setter
@NoArgsConstructor
public class OtpVerification extends BaseEntity {

 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "user_id")
 private User user;

 @Column(nullable = false, length = 160)
 private String email;

 @Enumerated(EnumType.STRING)
 @Column(nullable = false, length = 40)
 private OtpPurpose purpose;

 @Column(name = "operation_key", nullable = false, length = 120)
 private String operationKey;

 @Column(name = "operation_id", length = 120)
 private String legacyOperationId;

 @Column(name = "resend_available_at")
 private LocalDateTime legacyResendAvailableAt;

 @Column(name = "otp_hash", nullable = false)
 private String otpHash;

 @Column(name = "payload_json", columnDefinition = "TEXT")
 private String payloadJson;

 @Column(name = "expires_at", nullable = false)
 private LocalDateTime expiresAt;

 @Column(name = "last_sent_at", nullable = false)
 private LocalDateTime lastSentAt;

 @Column(nullable = false)
 private int attempts;

 @Column(nullable = false)
 private boolean used;

 @Column(nullable = false)
 private boolean verified;
}
