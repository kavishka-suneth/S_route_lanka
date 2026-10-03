package lk.smartroute.dto;
import jakarta.validation.constraints.*;
import lk.smartroute.model.*;
import java.math.BigDecimal;
import java.time.*;
public final class WalletDtos {
 private WalletDtos(){}
 public record WalletResponse(BigDecimal balance){}
 public record WalletTransactionResponse(Long id,String reference,WalletTransactionType type,WalletTransactionStatus status,BigDecimal amount,BigDecimal balanceAfter,Long bookingId,Instant createdAt){}
 public record CardResponse(Long cardId,String cardholderName,CardBrand brand,String maskedNumber,int expiryMonth,int expiryYear,boolean verified){}
 public record CardVerificationRequest(@NotBlank @Size(max=120) String cardholderName,CardBrand brand,@NotBlank @Pattern(regexp="\\d{13,19}") String cardNumber,@Min(1)@Max(12) int expiryMonth,@Min(2024) int expiryYear,@NotBlank @Pattern(regexp="\\d{3,4}") String cvv){}
 public record TopUpRequest(@NotNull Long cardId,@NotNull @DecimalMin("100.00") @DecimalMax("50000.00") BigDecimal amount){}
 public record OtpChallengeResponse(String operationKey,String message,LocalDateTime expiresAt){}
 public record OtpVerifyRequest(@NotBlank String operationKey,@NotBlank @Pattern(regexp="\\d{6}") String otp){}
 public record TopUpResponse(BigDecimal balance,WalletTransactionResponse transaction){}
 public record WalletPaymentResponse(ApiDtos.PaymentResponse payment,BigDecimal balance){}
 public record ForgotPasswordRequest(@Email@NotBlank String email){}
 public record ForgotPasswordVerifyRequest(@Email@NotBlank String email,@NotBlank@Pattern(regexp="\\d{6}") String otp){}
 public record ResetPasswordRequest(@Email@NotBlank String email,@NotBlank@Size(min=8,max=100) String newPassword,@NotBlank String confirmPassword){}
 public record MessageResponse(String message){}
}
