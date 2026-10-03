package lk.smartroute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.smartroute.model.OtpPurpose;
import lk.smartroute.repository.OtpVerificationRepository;
import lk.smartroute.service.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReportingAndAdministrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OtpVerificationRepository otpRepository;
    @MockBean EmailService emailService;

    @Test
    void reportingEndpointsCalculateSalesOccupancyFinancialsAndCsvExports() throws Exception {
        String passengerToken = register("reports-" + System.nanoTime() + "@smartroute.test", "Passenger123!");
        String adminToken = login("admin@smartroute.test", "AdminPass123!");
        Instant from = Instant.now().minusSeconds(5);

        long scheduleId = createSchedule(adminToken, new BigDecimal("100.00"));
        LocalDate travelDate = LocalDate.now().plusDays(5);

        long confirmedBooking = createBooking(passengerToken, scheduleId, Set.of(1, 2));
        pay(passengerToken, confirmedBooking, "CARD", "RPT-SUCCESS-" + System.nanoTime(), false);

        createBooking(passengerToken, scheduleId, Set.of(3));

        long refundedBooking = createBooking(passengerToken, scheduleId, Set.of(4));
        JsonNode refundedPayment = pay(passengerToken, refundedBooking, "CARD", "RPT-REFUND-" + System.nanoTime(), false);
        mockMvc.perform(post("/api/payments/{id}/refund", refundedPayment.get("paymentId").asLong()).header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));

        long failedBooking = createBooking(passengerToken, scheduleId, Set.of(5));
        pay(passengerToken, failedBooking, "CARD", "RPT-FAIL-" + System.nanoTime(), true);

        Instant to = Instant.now().plusSeconds(5);
        String fromParam = from.toString();
        String toParam = to.toString();

        mockMvc.perform(get("/api/reports/ticket-sales/details")
                .header("Authorization", bearer(adminToken))
                .param("scheduleId", String.valueOf(scheduleId))
                .param("from", travelDate.minusDays(1).toString())
                .param("to", travelDate.plusDays(1).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.totalTicketsSold").value(2))
            .andExpect(jsonPath("$.summary.totalBookings").value(1))
            .andExpect(jsonPath("$.summary.totalTicketSalesAmount").value(200.0))
            .andExpect(jsonPath("$.schedules.content[0].ticketsSold").value(2))
            .andExpect(jsonPath("$.schedules.content[0].remainingSeats").value(8));

        mockMvc.perform(get("/api/reports/schedule-occupancy/details")
                .header("Authorization", bearer(adminToken))
                .param("scheduleId", String.valueOf(scheduleId))
                .param("route", "Report, Origin")
                .param("travelDate", travelDate.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.schedules").value(1))
            .andExpect(jsonPath("$.schedules.content[0].occupiedSeats").value(3))
            .andExpect(jsonPath("$.schedules.content[0].availableSeats").value(7))
            .andExpect(jsonPath("$.schedules.content[0].occupancyPercent").value(30.0));

        mockMvc.perform(get("/api/reports/schedule-occupancy/details")
                .header("Authorization", bearer(adminToken))
                .param("from", "2099-01-01")
                .param("to", "2099-01-02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.schedules").value(0))
            .andExpect(jsonPath("$.summary.averageOccupancyPercent").value(0.0))
            .andExpect(jsonPath("$.schedules.content").isEmpty());

        mockMvc.perform(get("/api/reports/financial")
                .header("Authorization", bearer(adminToken))
                .param("from", fromParam)
                .param("to", toParam))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successfulRevenue").value(200.0))
            .andExpect(jsonPath("$.successfulPaymentCount").value(1))
            .andExpect(jsonPath("$.failedPaymentCount").value(1))
            .andExpect(jsonPath("$.refundedPaymentCount").value(1))
            .andExpect(jsonPath("$.refundedAmount").value(100.0))
            .andExpect(jsonPath("$.revenueByPaymentMethod[0].paymentMethod").value("CARD"));

        mockMvc.perform(get("/api/reports/revenue/details")
                .header("Authorization", bearer(adminToken))
                .param("from", fromParam)
                .param("to", toParam))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRevenue").value(200.0))
            .andExpect(jsonPath("$.refundedAmount").value(100.0));

        JsonNode management = jsonResponse(get("/api/reports/management")
            .header("Authorization", bearer(adminToken))
            .param("from", fromParam)
            .param("to", travelDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toString()));
        assertThat(management.get("routePerformance").get(0).get("route").asText()).contains("Report, Origin");
        assertThat(management.get("routePerformance").get(0).get("ticketsSold").asLong()).isEqualTo(2);

        String ticketCsv = textResponse(get("/api/reports/export/ticket-sales.csv")
            .header("Authorization", bearer(adminToken))
            .param("scheduleId", String.valueOf(scheduleId)));
        assertThat(ticketCsv).contains("scheduleId,route,travelDate");
        assertThat(ticketCsv).contains("\"Report, Origin");

        String managementCsv = textResponse(get("/api/reports/export/management.csv").header("Authorization", bearer(adminToken)));
        assertThat(managementCsv).contains("Bookings,Total bookings");

        String financialCsv = textResponse(get("/api/reports/export/financial.csv")
            .header("Authorization", bearer(adminToken))
            .param("from", fromParam)
            .param("to", toParam));
        assertThat(financialCsv).contains("Financial,Successful revenue,200.00");

        mockMvc.perform(get("/api/reports/financial")
                .header("Authorization", bearer(adminToken))
                .param("from", "2099-01-01T00:00:00Z")
                .param("to", "2099-01-02T00:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successfulRevenue").value(0.0))
            .andExpect(jsonPath("$.successfulPaymentCount").value(0));

        mockMvc.perform(get("/api/reports/financial")
                .header("Authorization", bearer(adminToken))
                .param("from", toParam)
                .param("to", fromParam))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("from must not be after to"));

        mockMvc.perform(get("/api/reports/financial").header("Authorization", bearer(passengerToken)))
            .andExpect(status().isForbidden());
    }

    private String register(String email, String password) throws Exception {
        clearInvocations(emailService);
        jsonResponse(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Report", "lastName", "Passenger", "email", email, "password", password, "phone", "0771234567"))));
        String otp = capturedOtp(email, OtpPurpose.REGISTRATION);
        mockMvc.perform(post("/api/auth/register/verify").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "otp", otp))))
            .andExpect(status().isOk());
        return login(email, password);
    }

    private String login(String email, String password) throws Exception {
        return jsonResponse(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", password)))).get("token").asText();
    }

    private long createSchedule(String adminToken, BigDecimal fare) throws Exception {
        long stamp = System.nanoTime();
        long routeId = jsonResponse(post("/api/routes").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("origin", "Report, Origin " + stamp, "destination", "Report Destination " + stamp)))).get("routeId").asLong();
        long busId = jsonResponse(post("/api/buses").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("registrationNo", "RPT-" + (stamp % 100000), "capacity", 10, "busType", "STANDARD", "availabilityStatus", "AVAILABLE", "status", "ACTIVE")))).get("busId").asLong();
        long driverId = jsonResponse(post("/api/drivers").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Report", "lastName", "Driver", "licenseNo", "RPT-LIC-" + stamp, "phone", "0711111111", "availabilityStatus", "AVAILABLE")))).get("driverId").asLong();
        LocalDate date = LocalDate.now().plusDays(5);
        LocalTime departure = LocalTime.of(10, 0);
        return jsonResponse(post("/api/schedules").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("routeId", routeId, "busId", busId, "driverId", driverId, "travelDate", date.toString(), "departureTime", departure.toString(), "arrivalTime", departure.plusHours(3).toString(), "fare", fare, "status", "SCHEDULED")))).get("scheduleId").asLong();
    }

    private long createBooking(String token, long scheduleId, Set<Integer> seats) throws Exception {
        return jsonResponse(post("/api/bookings").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("scheduleId", scheduleId, "seatNumbers", seats)))).get("bookingId").asLong();
    }

    private JsonNode pay(String token, long bookingId, String method, String reference, boolean fail) throws Exception {
        return jsonResponse(post("/api/payments/bookings/{id}", bookingId).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("paymentMethod", method, "transactionReference", reference, "simulateFailure", fail))));
    }

    private String capturedOtp(String email, OtpPurpose purpose) {
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendOtp(eq(email), otp.capture(), eq(purpose), anyInt());
        return otp.getAllValues().get(otp.getAllValues().size() - 1);
    }

    private JsonNode jsonResponse(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        String content = mockMvc.perform(request).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
    }

    private String textResponse(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
