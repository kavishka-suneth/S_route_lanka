package lk.smartroute.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lk.smartroute.model.*;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        }
    }

    public record RegisterRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Email @NotBlank @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 30) String phone
    ) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record AuthResponse(String token, String tokenType, long expiresInMs, UserResponse user) {
    }

    public record RegisterResponse(String message, String email, LocalDateTime otpExpiresAt, UserResponse user) {
    }

    public record RegistrationVerifyRequest(@Email @NotBlank String email, @NotBlank @Pattern(regexp = "\\d{6}") String otp) {
    }

    public record RegistrationResendRequest(@Email @NotBlank String email) {
    }

    public record UserResponse(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        UserType userType,
        Role role,
        StaffType staffType,
        Long passengerId,
        Long staffId,
        boolean emailVerified,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record ProfileUpdateRequest(@NotBlank @Size(max = 80) String firstName, @NotBlank @Size(max = 80) String lastName, @Size(max = 30) String phone) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 100) String newPassword) {
    }

    public record RoleUpdateRequest(@NotNull Role role) {
    }

    public record ActiveUpdateRequest(@NotNull Boolean active) {
    }

    public record RouteRequest(@NotBlank @Size(max = 120) String origin, @NotBlank @Size(max = 120) String destination, Boolean active) {
    }

    public record RouteResponse(Long routeId, String origin, String destination, boolean active) {
    }

    public record BusRequest(
        @NotBlank @Size(max = 30) String registrationNo,
        @Min(1) int capacity,
        @NotNull BusType busType,
        AvailabilityStatus availabilityStatus,
        BusStatus status
    ) {
    }

    public record AssignmentSummaryResponse(Long scheduleId, String route, LocalDate travelDate, LocalTime departureTime, LocalTime arrivalTime, ScheduleStatus status) {
    }

    public record BusResponse(
        Long busId,
        String registrationNo,
        int capacity,
        BusType busType,
        AvailabilityStatus availabilityStatus,
        BusStatus status,
        boolean assignedNow,
        AssignmentSummaryResponse nextAssignment
    ) {
    }

    public record SeatResponse(Long seatId, Long busId, int seatNumber, SeatType seatType) {
    }

    public record DriverRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Size(max = 60) String licenseNo,
        @Size(max = 30) @Pattern(regexp = "^$|^(?:\\+94|0)7\\d{8}$", message = "must be a valid Sri Lankan mobile number") String phone,
        AvailabilityStatus availabilityStatus
    ) {
    }

    public record DriverResponse(
        Long driverId,
        String firstName,
        String lastName,
        String licenseNo,
        String phone,
        AvailabilityStatus availabilityStatus,
        boolean assignedNow,
        AssignmentSummaryResponse nextAssignment
    ) {
    }

    public record ScheduleRequest(
        @NotNull Long routeId,
        @NotNull Long busId,
        @NotNull Long driverId,
        @NotNull LocalDate travelDate,
        @NotNull LocalTime departureTime,
        @NotNull LocalTime arrivalTime,
        @NotNull @DecimalMin(value = "0.01") BigDecimal fare,
        ScheduleStatus status
    ) {
    }

    public record ScheduleStatusUpdateRequest(
        @NotNull ScheduleStatus status,
        LocalTime updatedDepartureTime,
        @Size(max = 500) String delayReason
    ) {
        public ScheduleStatusUpdateRequest(ScheduleStatus status) {
            this(status, null, null);
        }
    }

    public record ScheduleResponse(
        Long scheduleId,
        RouteResponse route,
        BusResponse bus,
        DriverResponse driver,
        LocalDate travelDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        BigDecimal fare,
        ScheduleStatus status
    ) {
    }

    public record PassengerDetailRequest(Integer seatNumber, Boolean bookingForAnotherPassenger, @Size(max = 160) String fullName, @Size(max = 40) String nicOrPassport, @Size(max = 30) String phone, @Size(max = 160) String email) {}

    public record PassengerDetailResponse(int seatNumber, String fullName, String nicOrPassport, String phone, String email) {}

    public record BookingRequest(@NotNull Long scheduleId, @NotEmpty Set<@Min(1) Integer> seatNumbers, @Size(max = 100) List<@Valid PassengerDetailRequest> passengerDetails) {
        public BookingRequest(Long scheduleId, Set<Integer> seatNumbers) { this(scheduleId, seatNumbers, List.of()); }
    }

    public record ModifyBookingRequest(@NotEmpty Set<@Min(1) Integer> seatNumbers, @Size(max = 100) List<@Valid PassengerDetailRequest> passengerDetails) {
        public ModifyBookingRequest(Set<Integer> seatNumbers) { this(seatNumbers, List.of()); }
    }

    public record BookingResponse(
        Long bookingId,
        String bookingReference,
        UserResponse passengerUser,
        ScheduleResponse schedule,
        List<SeatResponse> seats,
        LocalDateTime bookingDate,
        BookingStatus bookingStatus,
        int passengerCount,
        BigDecimal totalAmount,
        LocalDateTime paymentDueAt,
        List<PassengerDetailResponse> passengerDetails,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record AvailableSeatsResponse(Long scheduleId, int capacity, List<SeatResponse> availableSeats) {
    }

    public record PaymentRequest(@NotBlank @Size(max = 100) String transactionReference, @NotBlank @Size(max = 40) String paymentMethod, Boolean simulateFailure) {
    }

    public record PaymentResponse(Long paymentId, String bookingReference, String paymentMethod, String transactionReference, LocalDateTime paymentDate, BigDecimal amount, PaymentStatus paymentStatus, BigDecimal refundedAmount) {
    }

    public record QrTicketResponse(String bookingReference, String qrCodeDataUri, LocalDateTime generatedAt) {}

    public record ReceiptResponse(String receiptNumber, String bookingReference, String passengerName, String route, List<SeatResponse> seats, List<PassengerDetailResponse> passengerDetails, BigDecimal amount, String paymentMethod, String transactionReference, LocalDateTime paymentDate) {
    }

    public record ReviewRequest(@NotNull Long bookingId, @Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {
    }

    public record ReviewResponse(Long reviewId, Long passengerId, Long bookingId, int rating, String comment, LocalDateTime reviewDate) {
    }

    public record SupportTicketRequest(@NotBlank @Size(max = 160) String subject, @NotBlank @Size(max = 2000) String description) {
    }

    public record SupportTicketResponseRequest(@Size(max = 2000) String response, @NotNull SupportTicketStatus status, Long handledByStaffId) {
    }

    public record SupportTicketStatusUpdateRequest(@NotNull SupportTicketStatus status) {
    }

    public record SupportTicketResponse(Long ticketId, Long userId, Long handledByStaffId, String subject, String description, SupportTicketStatus status, LocalDateTime createdDate) {
    }

    public record NotificationResponse(
        Long notificationId,
        Long userId,
        NotificationType notificationType,
        String title,
        String message,
        NotificationCategory category,
        String deliveryChannels,
        String relatedReference,
        String relatedEntityType,
        Long relatedEntityId,
        String actionUrl,
        NotificationPriority priority,
        LocalDateTime sentDateTime,
        LocalDateTime readAt,
        boolean archived,
        NotificationStatus status
    ) {
    }

    public record ManualNotificationRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 2000) String message,
        @NotNull NotificationType notificationType,
        @NotNull NotificationTargetType targetType,
        Long targetId,
        Set<Long> targetIds,
        NotificationCategory category,
        NotificationPriority priority,
        @Size(max = 300) String actionUrl
    ) {
    }

    public record NotificationPreferenceRequest(
        @NotNull Boolean emailEnabled,
        @NotNull Boolean smsEnabled,
        @NotNull Boolean inAppEnabled,
        Boolean bookingUpdatesEnabled,
        Boolean paymentUpdatesEnabled,
        Boolean scheduleChangesEnabled,
        Boolean tripRemindersEnabled,
        Boolean walletUpdatesEnabled,
        Boolean supportMessagesEnabled,
        Boolean securityAlertsEnabled,
        Boolean promotionalEnabled
    ) {
        public NotificationPreferenceRequest(Boolean emailEnabled, Boolean smsEnabled, Boolean inAppEnabled) {
            this(emailEnabled, smsEnabled, inAppEnabled, null, null, null, null, null, null, null, null);
        }
    }

    public record NotificationPreferenceResponse(
        boolean emailEnabled,
        boolean smsEnabled,
        boolean inAppEnabled,
        boolean bookingUpdatesEnabled,
        boolean paymentUpdatesEnabled,
        boolean scheduleChangesEnabled,
        boolean tripRemindersEnabled,
        boolean walletUpdatesEnabled,
        boolean supportMessagesEnabled,
        boolean securityAlertsEnabled,
        boolean promotionalEnabled
    ) {
    }

    public record RevenueReport(BigDecimal totalRevenue, long successfulPayments, long totalBookings) {
    }

    public record PaymentMethodRevenueReport(String paymentMethod, BigDecimal revenue, long successfulPayments) {
    }

    public record RevenueDetailsReport(BigDecimal totalRevenue, long successfulPayments, BigDecimal refundedAmount, long refundedTransactions, List<PaymentMethodRevenueReport> revenueByPaymentMethod) {
    }

    public record BookingCountReport(long totalBookings, long pendingPayment, long confirmed, long cancelled, long completed) {
    }

    public record TicketSalesReport(long activeReservedSeats) {
    }

    public record ScheduleOccupancyReport(Long scheduleId, String route, LocalDate travelDate, LocalTime departureTime, int capacity, long reservedSeats, double occupancyPercent) {
    }

    public record TicketSalesSummaryReport(long totalTicketsSold, long totalBookings, BigDecimal totalTicketSalesAmount) {
    }

    public record TicketSalesScheduleReport(Long scheduleId, String route, LocalDate travelDate, LocalTime departureTime, String busRegistrationNo, int busCapacity, long ticketsSold, long remainingSeats, double occupancyPercent, long bookingCount, BigDecimal ticketSalesAmount) {
    }

    public record TicketSalesDetailsReport(TicketSalesSummaryReport summary, PageResponse<TicketSalesScheduleReport> schedules) {
    }

    public record OccupancySummaryReport(long schedules, double averageOccupancyPercent) {
    }

    public record ScheduleOccupancyDetailReport(Long scheduleId, String route, LocalDate travelDate, LocalTime departureTime, String busRegistrationNo, int busCapacity, long occupiedSeats, long availableSeats, double occupancyPercent) {
    }

    public record OccupancyDetailsReport(OccupancySummaryReport summary, PageResponse<ScheduleOccupancyDetailReport> schedules) {
    }

    public record CancelledBookingsReport(long cancelledBookings) {
    }

    public record UserStatisticsReport(long totalUsers, long activeUsers, long passengers, long staff, long customerService, long transportCoordinators, long financeOfficers, long operationsManagers, long systemAdmins, long itSupport) {
    }

    public record PaymentStatisticsReport(long pending, long successful, long failed, long refunded) {
    }

    public record FinancialReport(BigDecimal successfulRevenue, long successfulPaymentCount, long pendingPaymentCount, long failedPaymentCount, long refundedPaymentCount, BigDecimal refundedAmount, List<PaymentMethodRevenueReport> revenueByPaymentMethod) {
    }

    public record ManagementRoutePerformanceReport(Long routeId, String route, long bookings, long ticketsSold, BigDecimal revenue, double averageOccupancyPercent) {
    }

    public record ManagementReport(long totalBookings, long pendingPaymentBookings, long confirmedBookings, long completedBookings, long cancelledBookings, long totalTicketsSold, long totalSchedules, double averageOccupancyPercent, BigDecimal successfulRevenue, long successfulPaymentCount, long cancelledBookingCount, List<ManagementRoutePerformanceReport> routePerformance) {
    }

    public record PopularRouteReport(Long routeId, String origin, String destination, long bookingCount) {
    }
}
