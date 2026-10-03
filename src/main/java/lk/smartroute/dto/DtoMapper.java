package lk.smartroute.dto;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.*;
import java.util.Comparator;
import java.util.List;

public final class DtoMapper {
    private DtoMapper() {
    }

    public static UserResponse user(User user) {
        Passenger passenger = user.getPassenger();
        Staff staff = user.getStaff();
        StaffType staffType = staff == null ? null : staff.getStaffType();
        return new UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getPhone(),
            user.getUserType(),
            user.getRole(),
            staffType,
            passenger == null ? null : passenger.getId(),
            staff == null ? null : staff.getId(),
            user.isEmailVerified(),
            user.isActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    public static RouteResponse route(Route route) {
        return new RouteResponse(route.getId(), route.getOrigin(), route.getDestination(), route.isActive());
    }

    public static BusResponse bus(Bus bus) {
        return bus(bus, false, null);
    }

    public static BusResponse bus(Bus bus, boolean assignedNow, AssignmentSummaryResponse nextAssignment) {
        return new BusResponse(
            bus.getId(),
            bus.getRegistrationNo(),
            bus.getCapacity(),
            bus.getBusType(),
            bus.getAvailabilityStatus(),
            bus.getStatus(),
            assignedNow,
            nextAssignment
        );
    }

    public static SeatResponse seat(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getBus().getId(), seat.getSeatNumber(), seat.getSeatType());
    }

    public static DriverResponse driver(Driver driver) {
        return driver(driver, false, null);
    }

    public static DriverResponse driver(Driver driver, boolean assignedNow, AssignmentSummaryResponse nextAssignment) {
        return new DriverResponse(
            driver.getId(),
            driver.getFirstName(),
            driver.getLastName(),
            driver.getLicenseNo(),
            driver.getPhone(),
            driver.getAvailabilityStatus(),
            assignedNow,
            nextAssignment
        );
    }

    public static ScheduleResponse schedule(Schedule schedule) {
        return new ScheduleResponse(
            schedule.getId(),
            route(schedule.getRoute()),
            bus(schedule.getBus()),
            driver(schedule.getDriver()),
            schedule.getTravelDate(),
            schedule.getDepartureTime(),
            schedule.getArrivalTime(),
            schedule.getFare(),
            schedule.getStatus()
        );
    }

    public static BookingResponse booking(Booking booking) {
        return new BookingResponse(
            booking.getId(),
            booking.getBookingReference(),
            user(booking.getPassenger().getUser()),
            schedule(booking.getSchedule()),
            seats(booking),
            booking.getBookingDate(),
            booking.getBookingStatus(),
            booking.getPassengerCount(),
            booking.getTotalAmount(),
            booking.getPaymentDueAt(),
            passengerDetails(booking),
            booking.getCreatedAt(),
            booking.getUpdatedAt()
        );
    }

    public static List<SeatResponse> seats(Booking booking) {
        return booking.getBookingSeats().stream()
            .map(BookingSeat::getSeat)
            .sorted(Comparator.comparingInt(Seat::getSeatNumber))
            .map(DtoMapper::seat)
            .toList();
    }

    public static List<PassengerDetailResponse> passengerDetails(Booking booking) {
        return booking.getPassengerDetails().stream()
            .sorted(Comparator.comparingInt(BookingPassenger::getSeatNumber))
            .map(passenger -> new PassengerDetailResponse(
                passenger.getSeatNumber(),
                passenger.getFullName(),
                passenger.getNicOrPassport(),
                passenger.getPhone(),
                passenger.getEmail()
            ))
            .toList();
    }

    public static PaymentResponse payment(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getBooking().getBookingReference(),
            payment.getPaymentMethod(),
            payment.getTransactionReference(),
            payment.getPaymentDate(),
            payment.getAmount(),
            payment.getPaymentStatus(),
            payment.getRefundedAmount()
        );
    }

    public static ReviewResponse review(Review review) {
        return new ReviewResponse(review.getId(), review.getPassenger().getId(), review.getBooking().getId(), review.getRating(), review.getComment(), review.getReviewDate());
    }

    public static SupportTicketResponse supportTicket(SupportTicket ticket) {
        return new SupportTicketResponse(
            ticket.getId(),
            ticket.getUser().getId(),
            ticket.getHandledByStaff() == null ? null : ticket.getHandledByStaff().getId(),
            ticket.getSubject(),
            ticket.getDescription(),
            ticket.getStatus(),
            ticket.getCreatedDate()
        );
    }

    public static NotificationResponse notification(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getUser().getId(),
            notification.getNotificationType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getCategory(),
            notification.getDeliveryChannels(),
            notification.getRelatedReference(),
            notification.getRelatedEntityType(),
            notification.getRelatedEntityId(),
            notification.getActionUrl(),
            notification.getPriority(),
            notification.getSentDateTime(),
            notification.getReadAt(),
            notification.isArchived(),
            notification.getStatus()
        );
    }

    public static String routeLabel(Schedule schedule) {
        return schedule.getRoute().getOrigin() + " to " + schedule.getRoute().getDestination();
    }

    public static AssignmentSummaryResponse assignment(Schedule schedule) {
        if (schedule == null) {
            return null;
        }
        return new AssignmentSummaryResponse(
            schedule.getId(),
            routeLabel(schedule),
            schedule.getTravelDate(),
            schedule.getDepartureTime(),
            schedule.getArrivalTime(),
            schedule.getStatus()
        );
    }
}
