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
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingPassengerDetailsTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean EmailService emailService;

    @Test
    void selfBookingStoresAccountOwnerForSelectedSeat() throws Exception {
        String email = "self-" + System.nanoTime() + "@smartroute.test";
        String token = register(email, "Self", "Passenger", "0771111111");
        long schedule = createSchedule(login("admin@smartroute.test", "AdminPass123!"));

        JsonNode booking = jsonResponse(post("/api/bookings")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("scheduleId", schedule, "seatNumbers", Set.of(1)))));

        assertThat(booking.get("passengerDetails")).hasSize(1);
        assertThat(booking.get("passengerDetails").get(0).get("seatNumber").asInt()).isEqualTo(1);
        assertThat(booking.get("passengerDetails").get(0).get("fullName").asText()).isEqualTo("Self Passenger");
        assertThat(booking.get("passengerDetails").get(0).get("phone").asText()).isEqualTo("0771111111");
        assertThat(booking.get("passengerDetails").get(0).get("email").asText()).isEqualTo(email);
    }

    @Test
    void friendBookingStoresFriendDetailsForSeat() throws Exception {
        String token = register("friend-owner-" + System.nanoTime() + "@smartroute.test", "Owner", "Passenger", "0772222222");
        long schedule = createSchedule(login("admin@smartroute.test", "AdminPass123!"));

        JsonNode booking = jsonResponse(post("/api/bookings")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "scheduleId", schedule,
                "seatNumbers", Set.of(2),
                "passengerDetails", List.of(Map.of(
                    "seatNumber", 2,
                    "bookingForAnotherPassenger", true,
                    "fullName", "Friend Rider",
                    "nicOrPassport", "NIC123",
                    "phone", "0773333333",
                    "email", "friend@example.com"
                ))
            ))));

        JsonNode passenger = booking.get("passengerDetails").get(0);
        assertThat(passenger.get("seatNumber").asInt()).isEqualTo(2);
        assertThat(passenger.get("fullName").asText()).isEqualTo("Friend Rider");
        assertThat(passenger.get("nicOrPassport").asText()).isEqualTo("NIC123");
        assertThat(passenger.get("email").asText()).isEqualTo("friend@example.com");
    }

    @Test
    void multipleSeatsKeepCorrectSeatPassengerMappingAndReceiptDetails() throws Exception {
        String email = "multi-" + System.nanoTime() + "@smartroute.test";
        String token = register(email, "Multi", "Owner", "0774444444");
        long schedule = createSchedule(login("admin@smartroute.test", "AdminPass123!"));

        JsonNode booking = jsonResponse(post("/api/bookings")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "scheduleId", schedule,
                "seatNumbers", Set.of(1, 3),
                "passengerDetails", List.of(
                    Map.of("seatNumber", 3, "bookingForAnotherPassenger", true, "fullName", "Mapped Friend", "email", "mapped.friend@example.com"),
                    Map.of("seatNumber", 1, "bookingForAnotherPassenger", false)
                )
            ))));

        Map<Integer, String> namesBySeat = namesBySeat(booking.get("passengerDetails"));
        assertThat(namesBySeat).containsEntry(1, "Multi Owner").containsEntry(3, "Mapped Friend");

        long bookingId = booking.get("bookingId").asLong();
        long paymentId = createPayment(token, bookingId);
        JsonNode receipt = jsonResponse(get("/api/payments/bookings/{bookingId}/receipt", bookingId)
            .header("Authorization", bearer(token)));
        assertThat(receipt.get("passengerDetails")).hasSize(2);
        assertThat(namesBySeat(receipt.get("passengerDetails"))).containsEntry(1, "Multi Owner").containsEntry(3, "Mapped Friend");
        assertThat(paymentId).isPositive();
    }

    @Test
    void friendPassengerValidationRejectsMissingNameInvalidEmailAndDuplicateSeatDetails() throws Exception {
        String token = register("validation-" + System.nanoTime() + "@smartroute.test", "Validation", "Owner", "0775555555");
        String admin = login("admin@smartroute.test", "AdminPass123!");

        long missingNameSchedule = createSchedule(admin);
        mockMvc.perform(post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "scheduleId", missingNameSchedule,
                    "seatNumbers", Set.of(1),
                    "passengerDetails", List.of(Map.of("seatNumber", 1, "bookingForAnotherPassenger", true))
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Passenger full name is required")));

        long invalidEmailSchedule = createSchedule(admin);
        mockMvc.perform(post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "scheduleId", invalidEmailSchedule,
                    "seatNumbers", Set.of(1),
                    "passengerDetails", List.of(Map.of("seatNumber", 1, "bookingForAnotherPassenger", true, "fullName", "Bad Email", "email", "not-an-email"))
                ))))
            .andExpect(status().isBadRequest());

        long duplicateSchedule = createSchedule(admin);
        mockMvc.perform(post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "scheduleId", duplicateSchedule,
                    "seatNumbers", Set.of(1),
                    "passengerDetails", List.of(
                        Map.of("seatNumber", 1, "bookingForAnotherPassenger", true, "fullName", "Friend One"),
                        Map.of("seatNumber", 1, "bookingForAnotherPassenger", true, "fullName", "Friend Two")
                    )
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Duplicate passenger details for seat 1")));
    }

    @Test
    void otherPassengerCannotViewPassengerDetails() throws Exception {
        String owner = register("owner-private-" + System.nanoTime() + "@smartroute.test", "Private", "Owner", "0776666666");
        String other = register("other-private-" + System.nanoTime() + "@smartroute.test", "Other", "Passenger", "0777777777");
        long schedule = createSchedule(login("admin@smartroute.test", "AdminPass123!"));
        JsonNode booking = jsonResponse(post("/api/bookings")
            .header("Authorization", bearer(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "scheduleId", schedule,
                "seatNumbers", Set.of(1),
                "passengerDetails", List.of(Map.of("seatNumber", 1, "bookingForAnotherPassenger", true, "fullName", "Private Friend"))
            ))));

        mockMvc.perform(get("/api/bookings/{id}", booking.get("bookingId").asLong())
                .header("Authorization", bearer(other)))
            .andExpect(status().isForbidden());
    }

    private Map<Integer, String> namesBySeat(JsonNode passengerDetails) {
        java.util.LinkedHashMap<Integer, String> names = new java.util.LinkedHashMap<>();
        passengerDetails.forEach(passenger -> names.put(passenger.get("seatNumber").asInt(), passenger.get("fullName").asText()));
        return names;
    }

    private String register(String email, String firstName, String lastName, String phone) throws Exception {
        clearInvocations(emailService);
        jsonResponse(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "email", email,
                "password", "Passenger123!",
                "phone", phone
            ))));
        String otp = capturedOtp(email);
        mockMvc.perform(post("/api/auth/register/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "otp", otp))))
            .andExpect(status().isOk());
        return login(email, "Passenger123!");
    }

    private String login(String email, String password) throws Exception {
        return jsonResponse(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", password))))
            .get("token").asText();
    }

    private long createSchedule(String adminToken) throws Exception {
        long stamp = System.nanoTime();
        long route = jsonResponse(post("/api/routes")
            .header("Authorization", bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("origin", "Origin" + stamp, "destination", "Destination" + stamp))))
            .get("routeId").asLong();
        long bus = jsonResponse(post("/api/buses")
            .header("Authorization", bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("registrationNo", "PD-" + (stamp % 1000000), "capacity", 12, "busType", "STANDARD", "availabilityStatus", "AVAILABLE", "status", "ACTIVE"))))
            .get("busId").asLong();
        long driver = jsonResponse(post("/api/drivers")
            .header("Authorization", bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Passenger", "lastName", "Driver", "licenseNo", "PD-LIC-" + stamp, "phone", "0711111111", "availabilityStatus", "AVAILABLE"))))
            .get("driverId").asLong();
        LocalDate travelDate = LocalDate.now().plusDays(8);
        LocalTime departure = LocalTime.of(10, 0);
        return jsonResponse(post("/api/schedules")
            .header("Authorization", bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "routeId", route,
                "busId", bus,
                "driverId", driver,
                "travelDate", travelDate.toString(),
                "departureTime", departure.toString(),
                "arrivalTime", departure.plusHours(3).toString(),
                "fare", new BigDecimal("800.00"),
                "status", "SCHEDULED"
            ))))
            .get("scheduleId").asLong();
    }

    private long createPayment(String token, long bookingId) throws Exception {
        JsonNode payment = jsonResponse(post("/api/payments/bookings/{bookingId}", bookingId)
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "transactionReference", "PD-TXN-" + bookingId,
                "paymentMethod", "CARD",
                "simulateFailure", false
            ))));
        return payment.get("paymentId").asLong();
    }

    private JsonNode jsonResponse(org.springframework.test.web.servlet.RequestBuilder requestBuilder) throws Exception {
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

    private String capturedOtp(String email) {
        org.mockito.ArgumentCaptor<String> otp = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendOtp(eq(email), otp.capture(), eq(OtpPurpose.REGISTRATION), anyInt());
        return otp.getAllValues().get(otp.getAllValues().size() - 1);
    }
}
