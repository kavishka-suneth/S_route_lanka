package lk.smartroute.controller;
import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.PageResponse;
import org.springframework.data.web.PageableDefault;
import lk.smartroute.dto.WalletDtos.*;
import lk.smartroute.service.WalletService;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/wallet") @PreAuthorize("hasRole('PASSENGER')")
public class WalletController{
 private final WalletService service;public WalletController(WalletService service){this.service=service;}
 @GetMapping public WalletResponse wallet(Authentication a){return service.getWallet(a.getName());}
 @GetMapping("/transactions") public PageResponse<WalletTransactionResponse> transactions(Authentication a,@PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC)Pageable p){return service.transactions(a.getName(),p);}
 @GetMapping("/cards") public List<CardResponse> cards(Authentication a){return service.cards(a.getName());}
 @PostMapping("/cards") public ResponseEntity<CardResponse> addCard(Authentication a,@Valid@RequestBody CardVerificationRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.addCard(a.getName(),r));}
 @PostMapping("/cards/otp") public OtpChallengeResponse requestCard(Authentication a,@Valid@RequestBody CardVerificationRequest r){return service.requestCardOtp(a.getName(),r);}
 @PostMapping("/cards/verify") public ResponseEntity<CardResponse> verifyCard(Authentication a,@Valid@RequestBody OtpVerifyRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.verifyCard(a.getName(),r));}
 @DeleteMapping("/cards/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(Authentication a,@PathVariable Long id){service.removeCard(a.getName(),id);}
 @PostMapping("/topups/otp") public OtpChallengeResponse topupOtp(Authentication a,@Valid@RequestBody TopUpRequest r){return service.requestTopUpOtp(a.getName(),r);}
 @PostMapping("/topups/confirm") public TopUpResponse topupConfirm(Authentication a,@Valid@RequestBody OtpVerifyRequest r){return service.confirmTopUp(a.getName(),r);}
 @PostMapping("/bookings/{id}/pay") public WalletPaymentResponse pay(Authentication a,@PathVariable Long id){return service.payBooking(a.getName(),id);}
}
