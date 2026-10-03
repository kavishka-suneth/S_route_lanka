package lk.smartroute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.smartroute.model.AvailabilityStatus;
import lk.smartroute.model.Driver;
import lk.smartroute.model.OtpPurpose;
import lk.smartroute.repository.DriverRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BusDriverAssignmentIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DriverRepository driverRepository;
    @MockBean EmailService emailService;

    @Test
    void createsAndEditsDriverAndRejectsDuplicateLicense() throws Exception {
        String admin = adminToken();
        long id = createDriver(admin, "LIC-" + System.nanoTime(), "0711111111", "AVAILABLE").get("driverId").asLong();

        mockMvc.perform(put("/api/drivers/{id}", id)
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "firstName", "Edited",
                    "lastName", "Driver",
                    "licenseNo", "LIC-EDIT-" + System.nanoTime(),
                    "phone", "+94712345678",
                    "availabilityStatus", "MAINTENANCE"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Edited"))
            .andExpect(jsonPath("$.availabilityStatus").value("MAINTENANCE"));

        String duplicate = "LIC-DUP-" + System.nanoTime();
        createDriver(admin, duplicate, "0711111111", "AVAILABLE");
        mockMvc.perform(post("/api/drivers")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("firstName", "Dup", "lastName", "Driver", "licenseNo", duplicate, "phone", "0711111111", "availabilityStatus", "AVAILABLE"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Driver license number already exists"));
    }

    @Test
    void createsAndEditsBusAndRejectsDuplicateRegistration() throws Exception {
        String admin = adminToken();
        long id = createBus(admin, "BUS-" + System.nanoTime(), 40, "AVAILABLE", "ACTIVE").get("busId").asLong();

        mockMvc.perform(put("/api/buses/{id}", id)
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "registrationNo", "BUS-EDIT-" + System.nanoTime(),
                    "capacity", 42,
                    "busType", "LUXURY",
                    "availabilityStatus", "MAINTENANCE",
                    "status", "MAINTENANCE"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.capacity").value(42))
            .andExpect(jsonPath("$.availabilityStatus").value("MAINTENANCE"));

        String duplicate = "BUS-DUP-" + System.nanoTime();
        createBus(admin, duplicate, 30, "AVAILABLE", "ACTIVE");
        mockMvc.perform(post("/api/buses")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("registrationNo", duplicate, "capacity", 30, "busType", "STANDARD", "availabilityStatus", "AVAILABLE", "status", "ACTIVE"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Bus registration number already exists"));
    }

    @Test
    void scheduleAssignmentConflictsAndAvailabilityEndpointsUseTimeWindows() throws Exception {
        String admin = adminToken();
        long route = createRoute(admin);
        long bus = createBus(admin, "BUS-TIME-" + System.nanoTime(), 30, "AVAILABLE", "ACTIVE").get("busId").asLong();
        long driver = createDriver(admin, "DRV-TIME-" + System.nanoTime(), "0711111111", "AVAILABLE").get("driverId").asLong();
        long otherBus = createBus(admin, "BUS-OTHER-" + System.nanoTime(), 30, "AVAILABLE", "ACTIVE").get("busId").asLong();
        long otherDriver = createDriver(admin, "DRV-OTHER-" + System.nanoTime(), "0711111111", "AVAILABLE").get("driverId").asLong();
        LocalDate date = LocalDate.now().plusDays(9);

        long schedule = createSchedule(admin, route, bus, driver, date, LocalTime.of(8, 0), LocalTime.of(10, 0), "SCHEDULED").get("scheduleId").asLong();

        mockMvc.perform(post("/api/schedules").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson(route, bus, otherDriver, date, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Bus is already assigned to an overlapping schedule"));

        mockMvc.perform(post("/api/schedules").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson(route, otherBus, driver, date, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Driver is already assigned to an overlapping schedule"));

        createSchedule(admin, route, otherBus, otherDriver, date, LocalTime.of(8, 0), LocalTime.of(10, 0), "SCHEDULED");
        createSchedule(admin, route, bus, driver, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "SCHEDULED");

        mockMvc.perform(get("/api/buses/available").header("Authorization", bearer(admin))
                .param("travelDate", date.toString()).param("departureTime", "09:00").param("arrivalTime", "09:30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.busId == " + bus + ")]").doesNotExist());

        mockMvc.perform(get("/api/drivers/available").header("Authorization", bearer(admin))
                .param("travelDate", date.toString()).param("departureTime", "09:00").param("arrivalTime", "09:30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.driverId == " + driver + ")]").doesNotExist());

        mockMvc.perform(put("/api/schedules/{id}", schedule).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson(route, bus, driver, date, LocalTime.of(8, 0), LocalTime.of(10, 0), "SCHEDULED")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/buses/available").header("Authorization", bearer(admin))
                .param("travelDate", date.toString()).param("departureTime", "08:00").param("arrivalTime", "10:00").param("excludeScheduleId", String.valueOf(schedule)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.busId == " + bus + ")]").exists());
    }

    @Test
    void cancelledAndCompletedSchedulesDoNotBlockAssignments() throws Exception {
        String admin = adminToken();
        long route = createRoute(admin);
        long bus = createBus(admin, "BUS-RELEASE-" + System.nanoTime(), 30, "AVAILABLE", "ACTIVE").get("busId").asLong();
        long driver = createDriver(admin, "DRV-RELEASE-" + System.nanoTime(), "0711111111", "AVAILABLE").get("driverId").asLong();
        LocalDate date = LocalDate.now().plusDays(10);

        long cancelled = createSchedule(admin, route, bus, driver, date, LocalTime.of(7, 0), LocalTime.of(9, 0), "SCHEDULED").get("scheduleId").asLong();
        mockMvc.perform(patch("/api/schedules/{id}/status", cancelled).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", "CANCELLED"))))
            .andExpect(status().isOk());
        createSchedule(admin, route, bus, driver, date, LocalTime.of(7, 0), LocalTime.of(9, 0), "SCHEDULED");

        long completedBus = createBus(admin, "BUS-COMP-" + System.nanoTime(), 30, "AVAILABLE", "ACTIVE").get("busId").asLong();
        long completedDriver = createDriver(admin, "DRV-COMP-" + System.nanoTime(), "0711111111", "AVAILABLE").get("driverId").asLong();
        createSchedule(admin, route, completedBus, completedDriver, date, LocalTime.of(11, 0), LocalTime.of(13, 0), "COMPLETED");
        createSchedule(admin, route, completedBus, completedDriver, date, LocalTime.of(11, 0), LocalTime.of(13, 0), "SCHEDULED");
    }

    @Test
    void unavailableMaintenanceResourcesAndManualAssignedAreRejected() throws Exception {
        String admin = adminToken();
        long route = createRoute(admin);
        LocalDate date = LocalDate.now().plusDays(11);
        long driver = createDriver(admin, "DRV-OK-" + System.nanoTime(), "0711111111", "AVAILABLE").get("driverId").asLong();
        long unavailableBus = createBus(admin, "BUS-UNAV-" + System.nanoTime(), 30, "UNAVAILABLE", "ACTIVE").get("busId").asLong();
        long maintenanceBus = createBus(admin, "BUS-MAINT-" + System.nanoTime(), 30, "MAINTENANCE", "ACTIVE").get("busId").asLong();
        long bus = createBus(admin, "BUS-OK-" + System.nanoTime(), 30, "AVAILABLE", "ACTIVE").get("busId").asLong();
        long unavailableDriver = createDriver(admin, "DRV-UNAV-" + System.nanoTime(), "0711111111", "UNAVAILABLE").get("driverId").asLong();
        long maintenanceDriver = createDriver(admin, "DRV-MAINT-" + System.nanoTime(), "0711111111", "MAINTENANCE").get("driverId").asLong();

        mockMvc.perform(post("/api/schedules").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson(route, unavailableBus, driver, date, LocalTime.of(8, 0), LocalTime.of(9, 0), "SCHEDULED")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Bus is unavailable or under maintenance"));
        mockMvc.perform(post("/api/schedules").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson(route, maintenanceBus, driver, date, LocalTime.of(9, 0), LocalTime.of(10, 0), "SCHEDULED")))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/schedules").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson(route, bus, unavailableDriver, date, LocalTime.of(10, 0), LocalTime.of(11, 0), "SCHEDULED")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Driver is unavailable or under maintenance"));
        mockMvc.perform(post("/api/schedules").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson(route, bus, maintenanceDriver, date, LocalTime.of(11, 0), LocalTime.of(12, 0), "SCHEDULED")))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/drivers").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("firstName", "Bad", "lastName", "State", "licenseNo", "DRV-ASSIGNED-" + System.nanoTime(), "phone", "0711111111", "availabilityStatus", "ASSIGNED"))))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/buses").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("registrationNo", "BUS-ASSIGNED-" + System.nanoTime(), "capacity", 30, "busType", "STANDARD", "availabilityStatus", "ASSIGNED", "status", "ACTIVE"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void futureAssignmentsProtectResourcesFromUnsafeStatusChanges() throws Exception {
        String admin = adminToken();
        long route = createRoute(admin);
        long bus = createBus(admin, "BUS-PROTECT-" + System.nanoTime(), 30, "AVAILABLE", "ACTIVE").get("busId").asLong();
        long driver = createDriver(admin, "DRV-PROTECT-" + System.nanoTime(), "0711111111", "AVAILABLE").get("driverId").asLong();
        LocalDate date = LocalDate.now().plusDays(12);
        createSchedule(admin, route, bus, driver, date, LocalTime.of(8, 0), LocalTime.of(9, 0), "SCHEDULED");

        mockMvc.perform(put("/api/buses/{id}", bus).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("registrationNo", "BUS-PROTECT-" + System.nanoTime(), "capacity", 30, "busType", "STANDARD", "availabilityStatus", "UNAVAILABLE", "status", "ACTIVE"))))
            .andExpect(status().isConflict());

        mockMvc.perform(put("/api/drivers/{id}", driver).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("firstName", "Protected", "lastName", "Driver", "licenseNo", "DRV-PROTECT-" + System.nanoTime(), "phone", "0711111111", "availabilityStatus", "MAINTENANCE"))))
            .andExpect(status().isConflict());
    }

    @Test
    void passengerSearchBookingRefundAndCompletedScheduleBehaviorRemainStable() throws Exception {
        String admin = adminToken();
        String passenger = registerPassenger("assignment-passenger-" + System.nanoTime() + "@smartroute.test");
        String origin = "PassengerOrigin" + System.nanoTime();
        String destination = "PassengerDest" + System.nanoTime();
        long route = createRoute(admin, origin, destination);
        long bus = createBus(admin, "BUS-PASS-" + System.nanoTime(), 30, "AVAILABLE", "ACTIVE").get("busId").asLong();
        JsonNode driverNode = createDriver(admin, "DRV-PASS-" + System.nanoTime(), "0711111111", "AVAILABLE");
        long driver = driverNode.get("driverId").asLong();
        LocalDate date = LocalDate.now().plusDays(13);
        long schedule = createSchedule(admin, route, bus, driver, date, LocalTime.of(8, 0), LocalTime.of(10, 0), "SCHEDULED").get("scheduleId").asLong();

        Driver legacyAssigned = driverRepository.findById(driver).orElseThrow();
        legacyAssigned.setAvailabilityStatus(AvailabilityStatus.ASSIGNED);
        driverRepository.save(legacyAssigned);

        mockMvc.perform(get("/api/schedules/search")
                .param("origin", origin)
                .param("destination", destination)
                .param("date", date.toString()))
            .andExpect(status().isOk());

        long booking = createBooking(passenger, schedule, Set.of(1));
        JsonNode payment = pay(passenger, booking, false);
        mockMvc.perform(patch("/api/schedules/{id}/status", schedule).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", "COMPLETED"))))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/payments/{paymentId}/verify", payment.get("paymentId").asLong()).header("Authorization", bearer(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
    }

    @Test
    void scheduleEditRejectsSmallerBusThanReservedSeatNumbers() throws Exception {
        String admin = adminToken();
        String passenger = registerPassenger("seat-capacity-" + System.nanoTime() + "@smartroute.test");
        long route = createRoute(admin);
        long originalBus = createBus(admin, "BUS-SEAT-OLD-" + System.nanoTime(), 5, "AVAILABLE", "ACTIVE").get("busId").asLong();
        long smallerBus = createBus(admin, "BUS-SEAT-NEW-" + System.nanoTime(), 4, "AVAILABLE", "ACTIVE").get("busId").asLong();
        long driver = createDriver(admin, "DRV-SEAT-" + System.nanoTime(), "0711111111", "AVAILABLE").get("driverId").asLong();
        LocalDate date = LocalDate.now().plusDays(14);
        long schedule = createSchedule(admin, route, originalBus, driver, date, LocalTime.of(8, 0), LocalTime.of(10, 0), "SCHEDULED").get("scheduleId").asLong();
        createBooking(passenger, schedule, Set.of(5));

        mockMvc.perform(put("/api/schedules/{id}", schedule).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson(route, smallerBus, driver, date, LocalTime.of(8, 0), LocalTime.of(10, 0), "SCHEDULED")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Cannot assign bus with capacity below existing reserved seat number 5"));
    }

    @Test
    void unauthorizedUsersCannotManageResourcesOrAvailabilityLists() throws Exception {
        String passenger = registerPassenger("unauthorized-" + System.nanoTime() + "@smartroute.test");

        mockMvc.perform(post("/api/buses").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("registrationNo", "NOAUTH-" + System.nanoTime(), "capacity", 30, "busType", "STANDARD", "availabilityStatus", "AVAILABLE", "status", "ACTIVE"))))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/drivers").header("Authorization", bearer(passenger)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("firstName", "No", "lastName", "Access", "licenseNo", "NOAUTH-" + System.nanoTime(), "phone", "0711111111", "availabilityStatus", "AVAILABLE"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/buses/available").header("Authorization", bearer(passenger))
                .param("travelDate", LocalDate.now().plusDays(2).toString()).param("departureTime", "08:00").param("arrivalTime", "09:00"))
            .andExpect(status().isForbidden());
    }

    private String adminToken() throws Exception {
        return login("admin@smartroute.test", "AdminPass123!");
    }

    private String registerPassenger(String email) throws Exception {
        clearInvocations(emailService);
        jsonResponse(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Test", "lastName", "Passenger", "email", email, "password", "Passenger123!", "phone", "0771234567"))));
        String otp = capturedOtp(email, OtpPurpose.REGISTRATION);
        mockMvc.perform(post("/api/auth/register/verify").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "otp", otp))))
            .andExpect(status().isOk());
        return login(email, "Passenger123!");
    }

    private String login(String email, String password) throws Exception {
        return jsonResponse(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", password)))).get("token").asText();
    }

    private long createRoute(String token) throws Exception {
        return createRoute(token, "Origin" + System.nanoTime(), "Destination" + System.nanoTime());
    }

    private long createRoute(String token, String origin, String destination) throws Exception {
        return jsonResponse(post("/api/routes").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("origin", origin, "destination", destination)))).get("routeId").asLong();
    }

    private JsonNode createBus(String token, String registration, int capacity, String availability, String status) throws Exception {
        return jsonResponse(post("/api/buses").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("registrationNo", registration, "capacity", capacity, "busType", "STANDARD", "availabilityStatus", availability, "status", status))));
    }

    private JsonNode createDriver(String token, String license, String phone, String availability) throws Exception {
        return jsonResponse(post("/api/drivers").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Test", "lastName", "Driver", "licenseNo", license, "phone", phone, "availabilityStatus", availability))));
    }

    private JsonNode createSchedule(String token, long route, long bus, long driver, LocalDate date, LocalTime departure, LocalTime arrival, String status) throws Exception {
        return jsonResponse(post("/api/schedules").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(scheduleJson(route, bus, driver, date, departure, arrival, status)));
    }

    private String scheduleJson(long route, long bus, long driver, LocalDate date, LocalTime departure, LocalTime arrival, String status) throws Exception {
        return json(Map.of("routeId", route, "busId", bus, "driverId", driver, "travelDate", date.toString(), "departureTime", departure.toString(), "arrivalTime", arrival.toString(), "fare", new BigDecimal("500.00"), "status", status));
    }

    private long createBooking(String token, long scheduleId, Set<Integer> seats) throws Exception {
        return jsonResponse(post("/api/bookings").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("scheduleId", scheduleId, "seatNumbers", seats)))).get("bookingId").asLong();
    }

    private JsonNode pay(String token, long bookingId, boolean fail) throws Exception {
        return jsonResponse(post("/api/payments/bookings/{id}", bookingId).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("paymentMethod", "CARD", "transactionReference", "ASSIGN-PAY-" + System.nanoTime(), "simulateFailure", fail))));
    }

    private JsonNode jsonResponse(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        String content = mockMvc.perform(request).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String capturedOtp(String email, OtpPurpose purpose) {
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendOtp(eq(email), otp.capture(), eq(purpose), anyInt());
        return otp.getAllValues().get(otp.getAllValues().size() - 1);
    }
}
