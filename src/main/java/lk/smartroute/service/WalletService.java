package lk.smartroute.service;
import lk.smartroute.dto.ApiDtos.PaymentResponse;
import lk.smartroute.dto.WalletDtos.*;
import org.springframework.data.domain.Pageable;
import lk.smartroute.dto.ApiDtos.PageResponse;
public interface WalletService{
 WalletResponse getWallet(String email);
 PageResponse<WalletTransactionResponse> transactions(String email,Pageable pageable);
 java.util.List<CardResponse> cards(String email);
 CardResponse addCard(String email,CardVerificationRequest request);
 OtpChallengeResponse requestCardOtp(String email,CardVerificationRequest request);
 CardResponse verifyCard(String email,OtpVerifyRequest request);
 void removeCard(String email,Long cardId);
 OtpChallengeResponse requestTopUpOtp(String email,TopUpRequest request);
 TopUpResponse confirmTopUp(String email,OtpVerifyRequest request);
 WalletPaymentResponse payBooking(String email,Long bookingId);
 default void refundWalletPayment(Long bookingId){ refundWalletPayment(bookingId, null); }
 void refundWalletPayment(Long bookingId, java.math.BigDecimal amount);
}
