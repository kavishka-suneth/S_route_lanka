package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.exception.ConflictException;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.*;
import lk.smartroute.repository.*;
import lk.smartroute.service.NotificationService;
import lk.smartroute.service.PaymentService;
import lk.smartroute.service.ScheduleService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ScheduleServiceImpl implements ScheduleService {
    private static final List<ScheduleStatus> ACTIVE_SCHEDULE_STATUSES = List.of(
        ScheduleStatus.SCHEDULED,
        ScheduleStatus.DELAYED
    );

    private final ScheduleRepository schedules;
    private final RouteRepository routes;
    private final BusRepository buses;
    private final DriverRepository drivers;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final SeatRepository seats;
    private final NotificationService notifications;
    private final PaymentService payments;

    public ScheduleServiceImpl(
        ScheduleRepository schedules,
        RouteRepository routes,
        BusRepository buses,
        DriverRepository drivers,
        BookingRepository bookings,
        BookingSeatRepository bookingSeats,
        SeatRepository seats,
        NotificationService notifications,
        PaymentService payments
    ) {
        this.schedules = schedules;
        this.routes = routes;
        this.buses = buses;
        this.drivers = drivers;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.seats = seats;
        this.notifications = notifications;
        this.payments = payments;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> search(String origin, String destination, LocalDate travelDate, Pageable pageable) {
        return PageResponse.from(schedules.searchScheduledTrips(origin, destination, travelDate, pageable).map(DtoMapper::schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> upcoming(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return PageResponse.from(schedules.findUpcomingSchedules(now.toLocalDate(), now.toLocalTime(), pageable).map(DtoMapper::schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> list(Pageable pageable) {
        return PageResponse.from(schedules.findAll(pageable).map(DtoMapper::schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse get(Long id) {
        return DtoMapper.schedule(scheduleById(id));
    }

    @Override
    public ScheduleResponse create(ScheduleRequest request, String actorEmail) {
        validateTimes(request);
        Route route = routes.findById(request.routeId())
            .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        Bus bus = buses.findByIdForUpdate(request.busId())
            .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));
        Driver driver = drivers.findByIdForUpdate(request.driverId())
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        ScheduleStatus status = request.status() == null ? ScheduleStatus.SCHEDULED : request.status();
        if (ACTIVE_SCHEDULE_STATUSES.contains(status)) {
            validateAssignable(route, bus, driver);
            assertFutureDeparture(request.travelDate(), request.departureTime());
            assertNoOverlap(null, bus.getId(), driver.getId(), request.travelDate(), request.departureTime(), request.arrivalTime());
        }

        Schedule schedule = new Schedule();
        schedule.setRoute(route);
        schedule.setBus(bus);
        schedule.setDriver(driver);
        schedule.setTravelDate(request.travelDate());
        schedule.setDepartureTime(request.departureTime());
        schedule.setArrivalTime(request.arrivalTime());
        schedule.setFare(request.fare());
        schedule.setStatus(status);
        Schedule saved = schedules.save(schedule);
        return DtoMapper.schedule(saved);
    }

    @Override
    public ScheduleResponse update(Long id, ScheduleRequest request, String actorEmail) {
        validateTimes(request);
        Schedule schedule = schedules.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        Long oldRouteId = schedule.getRoute().getId();
        String oldRoute = DtoMapper.routeLabel(schedule);
        String oldBusRegistration = schedule.getBus().getRegistrationNo();
        Long oldBusId = schedule.getBus().getId();
        LocalDate oldTravelDate = schedule.getTravelDate();
        java.time.LocalTime oldDeparture = schedule.getDepartureTime();
        java.time.LocalTime oldArrival = schedule.getArrivalTime();
        ScheduleStatus oldStatus = schedule.getStatus();
        Route route = routes.findById(request.routeId())
            .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        Bus bus = buses.findByIdForUpdate(request.busId())
            .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));
        Driver driver = drivers.findByIdForUpdate(request.driverId())
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        ScheduleStatus status = request.status() == null ? schedule.getStatus() : request.status();
        if (ACTIVE_SCHEDULE_STATUSES.contains(status)) {
            validateAssignable(route, bus, driver);
            assertFutureDeparture(request.travelDate(), request.departureTime());
            assertNoOverlap(id, bus.getId(), driver.getId(), request.travelDate(), request.departureTime(), request.arrivalTime());
        }
        assertExistingBookingsFitBus(schedule, bus);

        schedule.setRoute(route);
        boolean busChanged = !oldBusId.equals(bus.getId());
        schedule.setBus(bus);
        schedule.setDriver(driver);
        schedule.setTravelDate(request.travelDate());
        schedule.setDepartureTime(request.departureTime());
        schedule.setArrivalTime(request.arrivalTime());
        schedule.setFare(request.fare());
        schedule.setStatus(status);
        if (busChanged) {
            remapBookedSeatsToBus(schedule, bus);
            notifications.sendBusChanged(schedule, oldBusRegistration);
        }
        if (status == ScheduleStatus.DELAYED && (!oldDeparture.equals(request.departureTime()) || oldStatus != ScheduleStatus.DELAYED)) {
            if (schedule.getOriginalDepartureTime() == null) {
                schedule.setOriginalDepartureTime(oldDeparture);
            }
            notifications.sendScheduleDelay(schedule, oldDeparture, schedule.getDelayReason());
        } else {
            String summary = scheduleUpdateSummary(oldRouteId, oldRoute, oldTravelDate, oldDeparture, oldArrival, schedule, busChanged);
            if (summary != null) {
                notifications.sendScheduleUpdated(schedule, summary);
            }
        }
        return DtoMapper.schedule(schedule);
    }

    @Override
    public ScheduleResponse updateStatus(Long id, ScheduleStatus status, String actorEmail) {
        return updateStatus(id, new ScheduleStatusUpdateRequest(status, null, null), actorEmail);
    }

    @Override
    public ScheduleResponse updateStatus(Long id, ScheduleStatusUpdateRequest request, String actorEmail) {
        Schedule schedule = schedules.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        ScheduleStatus status = request.status();
        ScheduleStatus oldStatus = schedule.getStatus();
        java.time.LocalTime oldDeparture = schedule.getDepartureTime();
        if (request.updatedDepartureTime() != null) {
            if (!schedule.getArrivalTime().isAfter(request.updatedDepartureTime())) {
                throw new IllegalArgumentException("Updated departure time must be before arrival time");
            }
            schedule.setDepartureTime(request.updatedDepartureTime());
        }
        if (request.delayReason() != null && !request.delayReason().isBlank()) {
            schedule.setDelayReason(request.delayReason().trim());
        }
        if (ACTIVE_SCHEDULE_STATUSES.contains(status)) {
            buses.findByIdForUpdate(schedule.getBus().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));
            drivers.findByIdForUpdate(schedule.getDriver().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
            validateAssignable(schedule.getRoute(), schedule.getBus(), schedule.getDriver());
            assertFutureDeparture(schedule.getTravelDate(), schedule.getDepartureTime());
            assertNoOverlap(
                schedule.getId(),
                schedule.getBus().getId(),
                schedule.getDriver().getId(),
                schedule.getTravelDate(),
                schedule.getDepartureTime(),
                schedule.getArrivalTime()
            );
        }
        schedule.setStatus(status);
        if (status == ScheduleStatus.DELAYED && (oldStatus != ScheduleStatus.DELAYED || !oldDeparture.equals(schedule.getDepartureTime()))) {
            if (schedule.getOriginalDepartureTime() == null) {
                schedule.setOriginalDepartureTime(oldDeparture);
            }
            notifications.sendScheduleDelay(schedule, oldDeparture, schedule.getDelayReason());
        }
        if (status == ScheduleStatus.CANCELLED && oldStatus != ScheduleStatus.CANCELLED) {
            notifications.sendScheduleCancellation(schedule);
            cancelActiveBookings(schedule);
        }
        return DtoMapper.schedule(schedule);
    }

    @Override
    public void delete(Long id, String actorEmail) {
        updateStatus(id, ScheduleStatus.CANCELLED, actorEmail);
    }

    private void validateTimes(ScheduleRequest request) {
        if (!request.arrivalTime().isAfter(request.departureTime())) {
            throw new IllegalArgumentException("Arrival time must be after departure time");
        }
    }

    private void validateAssignable(Route route, Bus bus, Driver driver) {
        if (!route.isActive()) {
            throw new IllegalStateException("Route is inactive");
        }
        if (bus.getStatus() != BusStatus.ACTIVE || bus.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            throw new ConflictException("Bus is unavailable or under maintenance");
        }
        if (driver.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            throw new ConflictException("Driver is unavailable or under maintenance");
        }
    }

    private void assertFutureDeparture(LocalDate travelDate, java.time.LocalTime departure) {
        if (!LocalDateTime.of(travelDate, departure).isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Active schedules must depart in the future");
        }
    }

    private void assertNoOverlap(Long scheduleId, Long busId, Long driverId, LocalDate travelDate, java.time.LocalTime departure, java.time.LocalTime arrival) {
        long busOverlaps = schedules.countOverlappingBusAssignmentsExcluding(scheduleId, busId, travelDate, departure, arrival);
        if (busOverlaps > 0) {
            throw new ConflictException("Bus is already assigned to an overlapping schedule");
        }

        long driverOverlaps = schedules.countOverlappingDriverAssignmentsExcluding(scheduleId, driverId, travelDate, departure, arrival);
        if (driverOverlaps > 0) {
            throw new ConflictException("Driver is already assigned to an overlapping schedule");
        }
    }

    private void assertExistingBookingsFitBus(Schedule schedule, Bus bus) {
        int maxReservedSeat = bookingSeats.maxReservedSeatNumberForSchedule(schedule.getId());
        if (maxReservedSeat > bus.getCapacity()) {
            throw new ConflictException("Cannot assign bus with capacity below existing reserved seat number " + maxReservedSeat);
        }
    }

    private void remapBookedSeatsToBus(Schedule schedule, Bus bus) {
        for (BookingSeat bookingSeat : bookingSeats.findByScheduleId(schedule.getId())) {
            int seatNumber = bookingSeat.getSeat().getSeatNumber();
            Seat replacement = seats.findByBusIdAndSeatNumber(bus.getId(), seatNumber)
                .orElseThrow(() -> new ConflictException("Cannot preserve booked seat " + seatNumber + " on the selected bus"));
            bookingSeat.setSeat(replacement);
        }
    }

    private String scheduleUpdateSummary(
        Long oldRouteId,
        String oldRoute,
        LocalDate oldTravelDate,
        java.time.LocalTime oldDeparture,
        java.time.LocalTime oldArrival,
        Schedule schedule,
        boolean busChanged
    ) {
        List<String> changes = new java.util.ArrayList<>();
        if (!oldRouteId.equals(schedule.getRoute().getId())) {
            changes.add("Route changed from " + oldRoute + " to " + DtoMapper.routeLabel(schedule) + ".");
        }
        if (!oldTravelDate.equals(schedule.getTravelDate())) {
            changes.add("Travel date changed from " + oldTravelDate + " to " + schedule.getTravelDate() + ".");
        }
        if (!oldDeparture.equals(schedule.getDepartureTime())) {
            changes.add("Departure time changed from " + oldDeparture + " to " + schedule.getDepartureTime() + ".");
        }
        if (!oldArrival.equals(schedule.getArrivalTime())) {
            changes.add("Expected arrival time changed from " + oldArrival + " to " + schedule.getArrivalTime() + ".");
        }
        if (busChanged) {
            changes.add("Assigned bus changed to " + schedule.getBus().getRegistrationNo() + ".");
        }
        return changes.isEmpty() ? null : String.join(" ", changes);
    }

    private void cancelActiveBookings(Schedule schedule) {
        bookings.findByScheduleIdAndBookingStatusIn(schedule.getId(), List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED))
            .forEach(booking -> {
                if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
                    payments.refundSuccessfulPaymentForBooking(booking.getId(), "schedule-cancellation");
                } else {
                    booking.setBookingStatus(BookingStatus.CANCELLED);
                    booking.releaseSeats();
                    notifications.sendBookingCancellation(booking);
                }
            });
    }

    private Schedule scheduleById(Long id) {
        return schedules.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
    }
}
