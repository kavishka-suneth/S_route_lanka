package lk.smartroute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.smartroute.model.OtpPurpose;
import lk.smartroute.service.EmailService;
import org.junit.jupiter.api.Test;
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
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SmartRouteApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

    @Test
    void registrationLoginScheduleBookingPaymentAndReportsWork() throws Exception {
        String adminToken = login("admin@smartroute.test", "AdminPass123!");
        String passengerToken = registerPassenger("passenger1@smartroute.test");
        Long routeId = createRoute(adminToken);
        Long busId = createBus(adminToken);
        Long driverId = createDriver(adminToken);
        LocalDate travelDate = LocalDate.now().plusDays(3);
        LocalTime departureTime = LocalTime.of(9, 0);
        Long scheduleId = createSchedule(adminToken, routeId, busId, driverId, travelDate, departureTime);
        String otherPassengerToken = registerPassenger("other-" + System.nanoTime() + "@smartroute.test");

        mockMvc.perform(get("/api/schedules/upcoming"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].scheduleId").value(scheduleId));

        mockMvc.perform(get("/api/schedules/search")
                .param("origin", "Colombo")
                .param("destination", "Kandy")
                .param("date", travelDate.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].scheduleId").value(scheduleId));

        mockMvc.perform(get("/api/bookings/available-seats/{scheduleId}", scheduleId)
                .header("Authorization", bearer(passengerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.availableSeats[0].seatNumber").value(1));

        Long bookingId = createBooking(passengerToken, scheduleId, Set.of(1, 2));

        mockMvc.perform(get("/api/bookings")
                .header("Authorization", bearer(adminToken))
                .param("keyword", "Colombo")
                .param("status", "PENDING_PAYMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].bookingId").value(bookingId));

        mockMvc.perform(get("/api/bookings")
                .header("Authorization", bearer(otherPassengerToken)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/bookings")
                .header("Authorization", bearer(passengerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("scheduleId", scheduleId, "seatNumbers", Set.of(1)))))
            .andExpect(status().isConflict());

        Long paymentId = createPayment(passengerToken, bookingId);
        verify(emailService, timeout(5000).atLeastOnce()).sendTicket(eq("passenger1@smartroute.test"), any(), anyString());

        mockMvc.perform(get("/api/payments/{paymentId}/verify", paymentId)
                .header("Authorization", bearer(otherPassengerToken)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/payments")
                .header("Authorization", bearer(adminToken))
                .param("keyword", "TXN-" + bookingId)
                .param("status", "SUCCESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].paymentId").value(paymentId));

        mockMvc.perform(get("/api/payments")
                .header("Authorization", bearer(otherPassengerToken)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/payments/bookings/{bookingId}/receipt", bookingId)
                .header("Authorization", bearer(passengerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingReference").isString())
            .andExpect(jsonPath("$.seats.length()").value(2));

        mockMvc.perform(get("/api/reports/revenue/total")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successfulPayments").value(1));

        mockMvc.perform(get("/api/reports/export/summary.csv")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Total revenue")));

        mockMvc.perform(get("/api/payments/{paymentId}/verify", paymentId)
            .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        mockMvc.perform(patch("/api/bookings/{bookingId}/cancel", bookingId)
                .header("Authorization", bearer(passengerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));

        mockMvc.perform(get("/api/payments/{paymentId}/verify", paymentId)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));

        Long failedBookingId = createBooking(passengerToken, scheduleId, Set.of(3));
        JsonNode failedPayment = performJson(post("/api/payments/bookings/{bookingId}", failedBookingId)
            .header("Authorization", bearer(passengerToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "transactionReference", "FAIL-" + failedBookingId,
                "paymentMethod", "CARD",
                "simulateFailure", true
            ))));
        assertThat(failedPayment.get("paymentStatus").asText()).isEqualTo("FAILED");

        mockMvc.perform(get("/api/bookings/{id}", failedBookingId)
                .header("Authorization", bearer(passengerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));

        JsonNode availableAfterFailure = performJson(get("/api/bookings/available-seats/{scheduleId}", scheduleId)
            .header("Authorization", bearer(passengerToken)));
        boolean seatReleased = false;
        for (JsonNode seat : availableAfterFailure.get("availableSeats")) {
            if (seat.get("seatNumber").asInt() == 3) {
                seatReleased = true;
                break;
            }
        }
        assertThat(seatReleased).isTrue();
    }

    @Test
    void registrationResponseDoesNotExposePasswordHash() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "firstName", "Safe",
                    "lastName", "Passenger",
                    "email", "safe-passenger@smartroute.test",
                    "password", "Passenger123!",
                    "phone", "0770000000"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.user.password").doesNotExist())
            .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Passenger123!"))));
    }

    @Test
    void staticAuthenticatedPagesLoadAndInactiveUsersCannotLogin() throws Exception {
        mockMvc.perform(get("/my-bookings.html"))
            .andExpect(status().isOk());

        String adminToken = login("admin@smartroute.test", "AdminPass123!");
        JsonNode registered = performJson(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "firstName", "Inactive",
                "lastName", "Passenger",
                "email", "inactive-" + System.nanoTime() + "@smartroute.test",
                "password", "Passenger123!",
                "phone", "0771234567"
            ))));
        long userId = registered.get("user").get("userId").asLong();
        mockMvc.perform(patch("/api/users/{id}/active", userId)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("active", false))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "email", registered.get("user").get("email").asText(),
                    "password", "Passenger123!"
                ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void staffRoleFilteringUsesActualStaffRole() throws Exception {
        String adminToken = login("admin@smartroute.test", "AdminPass123!");

        mockMvc.perform(get("/api/users")
                .header("Authorization", bearer(adminToken))
                .param("role", "SYSTEM_ADMIN")
                .param("active", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].role").value("SYSTEM_ADMIN"));
    }

    private String registerPassenger(String email) throws Exception {
        clearInvocations(emailService);
        performJson(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                "firstName", "Test",
                "lastName", "Passenger",
                "email", email,
                "password", "Passenger123!",
                "phone", "0771234567"
            ))));
        String otp = capturedOtp(email, OtpPurpose.REGISTRATION);
        mockMvc.perform(post("/api/auth/register/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "otp", otp))))
            .andExpect(status().isOk());
        return login(email, "Passenger123!");
    }

    private String login(String email, String password) throws Exception {
        JsonNode response = performJson(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", password))));
        return response.get("token").asText();
    }

    private Long createRoute(String token) throws Exception {
        JsonNode response = performJson(post("/api/routes")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                "origin", "Colombo",
                "destination", "Kandy"
            ))));
        return response.get("routeId").asLong();
    }

    private Long createBus(String token) throws Exception {
        JsonNode response = performJson(post("/api/buses")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                "registrationNo", "NB-1234",
                "capacity", 40,
                "busType", "STANDARD",
                "availabilityStatus", "AVAILABLE",
                "status", "ACTIVE"
            ))));
        return response.get("busId").asLong();
    }

    private Long createDriver(String token) throws Exception {
        JsonNode response = performJson(post("/api/drivers")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                "firstName", "Nimal",
                "lastName", "Perera",
                "licenseNo", "B1234567",
                "phone", "0711234567",
                "availabilityStatus", "AVAILABLE"
            ))));
        return response.get("driverId").asLong();
    }

    private Long createSchedule(String token, Long routeId, Long busId, Long driverId, LocalDate travelDate, LocalTime departureTime) throws Exception {
        JsonNode response = performJson(post("/api/schedules")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "routeId", routeId,
                "busId", busId,
                "driverId", driverId,
                "travelDate", travelDate.toString(),
                "departureTime", departureTime.toString(),
                "arrivalTime", departureTime.plusHours(4).toString(),
                "fare", new BigDecimal("950.00"),
                "status", "SCHEDULED"
            ))));
        assertThat(LocalDate.parse(response.get("travelDate").asText())).isEqualTo(travelDate);
        return response.get("scheduleId").asLong();
    }

    private Long createBooking(String token, Long scheduleId, Set<Integer> seats) throws Exception {
        JsonNode response = performJson(post("/api/bookings")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("scheduleId", scheduleId, "seatNumbers", seats))));
        return response.get("bookingId").asLong();
    }

    private Long createPayment(String token, Long bookingId) throws Exception {
        JsonNode response = performJson(post("/api/payments/bookings/{bookingId}", bookingId)
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                "transactionReference", "TXN-" + bookingId,
                "paymentMethod", "CARD",
                "simulateFailure", false
            ))));
        assertThat(response.get("paymentStatus").asText()).isEqualTo("SUCCESS");
        return response.get("paymentId").asLong();
    }

    private JsonNode performJson(org.springframework.test.web.servlet.RequestBuilder requestBuilder) throws Exception {
        String content = mockMvc.perform(requestBuilder)
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(content);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String capturedOtp(String email, OtpPurpose purpose) {
        org.mockito.ArgumentCaptor<String> otp = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendOtp(eq(email), otp.capture(), eq(purpose), anyInt());
        return otp.getAllValues().get(otp.getAllValues().size() - 1);
    }
}
