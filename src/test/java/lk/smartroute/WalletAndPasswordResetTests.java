package lk.smartroute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.smartroute.model.OtpPurpose;
import lk.smartroute.model.OtpVerification;
import lk.smartroute.repository.OtpVerificationRepository;
import lk.smartroute.service.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WalletAndPasswordResetTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OtpVerificationRepository otpRepository;
    @MockBean EmailService emailService;

    @Test
    void registrationRequiresEmailVerificationBeforeLogin() throws Exception {
        String email = "verify-" + System.nanoTime() + "@smartroute.test";
        clearInvocations(emailService);
        jsonResponse(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName","Email","lastName","Verifier","email",email,"password","Passenger123!","phone","0771234567"))));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email",email,"password","Passenger123!"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Please verify your email before logging in"));
        mockMvc.perform(post("/api/auth/register/resend").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email",email))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Please wait before requesting another verification code"));
        String otp = capturedOtp(email, OtpPurpose.REGISTRATION);
        mockMvc.perform(post("/api/auth/register/verify").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email",email,"otp",otp))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Email verified successfully. Please log in."));
        mockMvc.perform(post("/api/auth/register/verify").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email",email,"otp",otp))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Email is already verified. Please log in."));
        login(email, "Passenger123!");
    }

    @Test
    void walletCardTopupBookingRefundAndOwnershipFlow() throws Exception {
        String email = "wallet-" + System.nanoTime() + "@smartroute.test";
        String token = register(email, "Passenger123!");
        String otherToken = register("other-wallet-" + System.nanoTime() + "@smartroute.test", "Passenger123!");

        mockMvc.perform(get("/api/wallet").header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(0.0));

        JsonNode card = jsonResponse(post("/api/wallet/cards")
            .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("cardholderName","Demo Passenger","brand","VISA","cardNumber","4111111111111111","expiryMonth",12,"expiryYear",2030,"cvv","123"))));
        long cardId = card.get("cardId").asLong();
        assertThat(card.get("brand").asText()).isEqualTo("VISA");
        assertThat(card.get("maskedNumber").asText()).isEqualTo("**** **** **** 1111");

        mockMvc.perform(post("/api/wallet/cards").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("cardholderName","Mismatch Passenger","brand","MASTERCARD","cardNumber","4111111111111111","expiryMonth",12,"expiryYear",2030,"cvv","123"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Selected card brand does not match the card number"));

        mockMvc.perform(post("/api/wallet/cards").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("cardholderName","Declined Passenger","brand","VISA","cardNumber","4000000000000002","expiryMonth",12,"expiryYear",2030,"cvv","123"))))
            .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/wallet/cards/{id}", cardId).header("Authorization", bearer(otherToken)))
            .andExpect(status().isNotFound());

        clearInvocations(emailService);
        JsonNode topupChallenge = jsonResponse(post("/api/wallet/topups/otp")
            .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("cardId",cardId,"amount",new BigDecimal("2000.00")))));
        String topupOtp = capturedOtp(email, OtpPurpose.WALLET_TOP_UP);
        String topupOperation = topupChallenge.get("operationKey").asText();
        mockMvc.perform(post("/api/wallet/topups/confirm").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("operationKey",topupOperation,"otp",topupOtp))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(2000.0));
        mockMvc.perform(post("/api/wallet/topups/confirm").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("operationKey",topupOperation,"otp",topupOtp))))
            .andExpect(status().isBadRequest());

        String admin = login("admin@smartroute.test", "AdminPass123!");
        long scheduleId = createSchedule(admin, new BigDecimal("950.00"));
        long bookingId = createBooking(token, scheduleId, Set.of(1));
        mockMvc.perform(post("/api/wallet/bookings/{id}/pay", bookingId).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.payment.paymentStatus").value("SUCCESS")).andExpect(jsonPath("$.balance").value(1050.0));
        verify(emailService, timeout(5000).atLeastOnce()).sendTicket(eq(email), any(), anyString());
        mockMvc.perform(post("/api/wallet/bookings/{id}/pay", bookingId).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/bookings/{id}/cancel", bookingId).header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));
        mockMvc.perform(get("/api/wallet").header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(2000.0));
        mockMvc.perform(patch("/api/bookings/{id}/cancel", bookingId).header("Authorization", bearer(token)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/wallet").header("Authorization", bearer(token)))
            .andExpect(jsonPath("$.balance").value(2000.0));

        long expensiveBooking = createBooking(token, scheduleId, Set.of(2,3,4));
        mockMvc.perform(post("/api/wallet/bookings/{id}/pay", expensiveBooking).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Insufficient wallet balance"));
    }

    @Test
    void otpMaximumAttemptsAndExpiryAreEnforced() throws Exception {
        String email = "otp-" + System.nanoTime() + "@smartroute.test";
        String token = register(email, "Passenger123!");
        clearInvocations(emailService);
        JsonNode challenge = jsonResponse(post("/api/wallet/cards/otp").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("cardholderName","Demo","brand","MASTERCARD","cardNumber","5555555555554444","expiryMonth",11,"expiryYear",2031,"cvv","123"))));
        String operation = challenge.get("operationKey").asText();
        String realOtp = capturedOtp(email, OtpPurpose.CARD_VERIFICATION);
        String wrongOtp = realOtp.equals("000000") ? "000001" : "000000";
        for (int i=0;i<5;i++) mockMvc.perform(post("/api/wallet/cards/verify").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("operationKey",operation,"otp",wrongOtp)))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/wallet/cards/verify").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("operationKey",operation,"otp",realOtp)))).andExpect(status().isBadRequest());

        Thread.sleep(5);
        // create a new challenge after making the previous active OTP old enough to bypass the test cooldown
        OtpVerification latest = otpRepository.findFirstByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, OtpPurpose.CARD_VERIFICATION).orElse(null);
        if (latest != null) { latest.setUsed(true); otpRepository.saveAndFlush(latest); }
        clearInvocations(emailService);
        JsonNode exp = jsonResponse(post("/api/wallet/cards/otp").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("cardholderName","Demo","brand","MASTERCARD","cardNumber","5555555555554444","expiryMonth",11,"expiryYear",2031,"cvv","123"))));
        String expOtp = capturedOtp(email, OtpPurpose.CARD_VERIFICATION);
        OtpVerification active = otpRepository.findFirstByEmailIgnoreCaseAndPurposeAndOperationKeyAndUsedFalseOrderByCreatedAtDesc(email,OtpPurpose.CARD_VERIFICATION,exp.get("operationKey").asText()).orElseThrow();
        active.setExpiresAt(LocalDateTime.now().minusSeconds(1)); otpRepository.saveAndFlush(active);
        mockMvc.perform(post("/api/wallet/cards/verify").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("operationKey",exp.get("operationKey").asText(),"otp",expOtp)))).andExpect(status().isBadRequest());
    }

    @Test
    void forgotPasswordIsNeutralAndNewPasswordCanLogin() throws Exception {
        String email = "reset-" + System.nanoTime() + "@smartroute.test";
        register(email, "Passenger123!");
        String neutral = "If an account exists for this email, a verification code has been sent.";
        mockMvc.perform(post("/api/auth/forgot-password/request").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email","missing-"+System.nanoTime()+"@smartroute.test"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value(neutral));

        clearInvocations(emailService);
        mockMvc.perform(post("/api/auth/forgot-password/request").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email",email))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value(neutral));
        String otp = capturedOtp(email, OtpPurpose.PASSWORD_RESET);
        mockMvc.perform(post("/api/auth/forgot-password/verify").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email",email,"otp",otp))))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/forgot-password/reset").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email",email,"newPassword","NewPass123!","confirmPassword","NewPass123!"))))
            .andExpect(status().isOk());
        login(email, "NewPass123!");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email",email,"password","Passenger123!"))))
            .andExpect(status().isBadRequest());

        clearInvocations(emailService);
        mockMvc.perform(post("/api/auth/forgot-password/request").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email",email))))
            .andExpect(status().isOk());
        String expiredOtp = capturedOtp(email, OtpPurpose.PASSWORD_RESET);
        OtpVerification active = otpRepository.findFirstByEmailIgnoreCaseAndPurposeAndOperationKeyAndUsedFalseOrderByCreatedAtDesc(email,OtpPurpose.PASSWORD_RESET,"PASSWORD_RESET").orElseThrow();
        active.setExpiresAt(LocalDateTime.now().minusSeconds(1)); otpRepository.saveAndFlush(active);
        mockMvc.perform(post("/api/auth/forgot-password/verify").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email",email,"otp",expiredOtp))))
            .andExpect(status().isBadRequest());
    }

    private String capturedOtp(String email, OtpPurpose purpose) {
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendOtp(eq(email), otp.capture(), eq(purpose), anyInt());
        return otp.getAllValues().get(otp.getAllValues().size()-1);
    }
    private String register(String email,String password) throws Exception {
        clearInvocations(emailService);
        jsonResponse(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("firstName","Wallet","lastName","Tester","email",email,"password",password,"phone","0771234567"))));
        String otp = capturedOtp(email, OtpPurpose.REGISTRATION);
        mockMvc.perform(post("/api/auth/register/verify").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email",email,"otp",otp))))
            .andExpect(status().isOk());
        return login(email,password);
    }
    private String login(String email,String password) throws Exception {return jsonResponse(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email",email,"password",password)))).get("token").asText();}
    private long createSchedule(String admin,BigDecimal fare) throws Exception {long route=jsonResponse(post("/api/routes").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("origin","WalletOrigin"+System.nanoTime(),"destination","WalletDest"+System.nanoTime())))).get("routeId").asLong();long bus=jsonResponse(post("/api/buses").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("registrationNo","WB-"+(System.nanoTime()%100000),"capacity",10,"busType","STANDARD","availabilityStatus","AVAILABLE","status","ACTIVE")))).get("busId").asLong();long driver=jsonResponse(post("/api/drivers").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("firstName","Wallet","lastName","Driver","licenseNo","WL"+System.nanoTime(),"phone","0711111111","availabilityStatus","AVAILABLE")))).get("driverId").asLong();LocalDate d=LocalDate.now().plusDays(5);LocalTime t=LocalTime.of(10,0);return jsonResponse(post("/api/schedules").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("routeId",route,"busId",bus,"driverId",driver,"travelDate",d.toString(),"departureTime",t.toString(),"arrivalTime",t.plusHours(3).toString(),"fare",fare,"status","SCHEDULED")))).get("scheduleId").asLong();}
    private long createBooking(String token,long scheduleId,Set<Integer> seats) throws Exception {return jsonResponse(post("/api/bookings").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("scheduleId",scheduleId,"seatNumbers",seats)))).get("bookingId").asLong();}
    private JsonNode jsonResponse(org.springframework.test.web.servlet.RequestBuilder b) throws Exception {String c=mockMvc.perform(b).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();return objectMapper.readTree(c);}
    private String json(Object o) throws Exception{return objectMapper.writeValueAsString(o);} private String bearer(String t){return "Bearer "+t;}
}
