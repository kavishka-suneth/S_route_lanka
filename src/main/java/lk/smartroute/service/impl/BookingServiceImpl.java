package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.exception.ConflictException;
import lk.smartroute.exception.ForbiddenActionException;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.*;
import lk.smartroute.repository.*;
import lk.smartroute.service.BookingService;
import lk.smartroute.service.NotificationService;
import lk.smartroute.service.PaymentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final ScheduleRepository schedules;
    private final SeatRepository seats;
    private final UserRepository users;
    private final PassengerRepository passengers;
    private final NotificationService notifications;
    private final PaymentService payments;
    @Value("${app.booking.hold-minutes:10}") private long holdMinutes;
    @Value("${app.cancellation.full-refund-hours:24}") private long fullRefundHours;
    @Value("${app.cancellation.partial-refund-hours:6}") private long partialRefundHours;
    @Value("${app.cancellation.partial-refund-percent:50}") private int partialRefundPercent;

    public BookingServiceImpl(
        BookingRepository bookings,
        BookingSeatRepository bookingSeats,
        ScheduleRepository schedules,
        SeatRepository seats,
        UserRepository users,
        PassengerRepository passengers,
        NotificationService notifications,
        PaymentService payments
    ) {
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.schedules = schedules;
        this.seats = seats;
        this.users = users;
        this.passengers = passengers;
        this.notifications = notifications;
        this.payments = payments;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listAll(BookingStatus status, String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return PageResponse.from(bookings.searchBookings(status, normalizedKeyword, pageable).map(DtoMapper::booking));
    }

    @Override
    public BookingResponse create(String email, BookingRequest request) {
        Passenger passenger = passengerByEmail(email);
        Schedule schedule = schedules.findByIdForUpdate(request.scheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        assertBookable(schedule);

        List<Integer> requestedSeatNumbers = normalizedSeats(request.seatNumbers(), schedule.getBus().getCapacity());
        List<Seat> requestedSeats = seatsForSchedule(schedule, requestedSeatNumbers);
        Set<Long> occupiedSeatIds = bookingSeats.findReservedSeatIdsByScheduleId(schedule.getId());
        assertSeatsAvailable(requestedSeats, occupiedSeatIds);

        Booking booking = new Booking();
        booking.setBookingReference(generateReference());
        booking.setPassenger(passenger);
        booking.setSchedule(schedule);
        booking.setBookingDate(LocalDateTime.now());
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentDueAt(LocalDateTime.now().plusMinutes(holdMinutes));
        booking.setTotalAmount(schedule.getFare().multiply(BigDecimal.valueOf(requestedSeats.size())));
        booking.replaceSeats(schedule, requestedSeats);
        booking.replacePassengerDetails(passengerDetailsFor(booking, passenger.getUser(), requestedSeatNumbers, request.passengerDetails()));

        try {
            Booking saved = bookings.saveAndFlush(booking);
            return DtoMapper.booking(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("One or more selected seats were just reserved by another booking");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse get(String email, Long bookingId, boolean staffAccess) {
        Booking booking = bookingById(bookingId);
        verifyOwnerOrStaff(booking, email, staffAccess);
        return DtoMapper.booking(booking);
    }

    @Override
    public BookingResponse cancel(String email, Long bookingId, boolean staffAccess) {
        Booking booking = bookingById(bookingId);
        verifyOwnerOrStaff(booking, email, staffAccess);
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Completed booking cannot be cancelled");
        }
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            return DtoMapper.booking(booking);
        }
        if (booking.getSchedule().getDepartureDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Departed bookings cannot be cancelled");
        }
        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            long hours = Duration.between(LocalDateTime.now(), booking.getSchedule().getDepartureDateTime()).toHours();
            if (hours < partialRefundHours) throw new IllegalStateException("Cancellation is not eligible within " + partialRefundHours + " hours of departure");
            BigDecimal refund = booking.getTotalAmount().multiply(BigDecimal.valueOf(hours >= fullRefundHours ? 100 : partialRefundPercent)).divide(BigDecimal.valueOf(100));
            payments.refundSuccessfulPaymentForBooking(booking.getId(), email, refund.setScale(2, java.math.RoundingMode.HALF_UP));
        } else {
            booking.setBookingStatus(BookingStatus.CANCELLED);
            booking.releaseSeats();
            notifications.sendBookingCancellation(booking);
        }
        return DtoMapper.booking(booking);
    }

    @Override
    public BookingResponse modifySeats(String email, Long bookingId, ModifyBookingRequest request) {
        Booking booking = bookingById(bookingId);
        verifyOwnerOrStaff(booking, email, false);
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only bookings pending payment can be modified");
        }
        String previousSeats = seatsLabel(booking);
        Schedule schedule = schedules.findByIdForUpdate(booking.getSchedule().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        assertBookable(schedule);

        List<Integer> requestedSeatNumbers = normalizedSeats(request.seatNumbers(), schedule.getBus().getCapacity());
        List<Seat> requestedSeats = seatsForSchedule(schedule, requestedSeatNumbers);
        Set<Long> occupiedSeatIds = bookingSeats.findReservedSeatIdsByScheduleIdExcludingBooking(schedule.getId(), booking.getId());
        assertSeatsAvailable(requestedSeats, occupiedSeatIds);

        booking.releaseSeats();
        bookings.flush();
        booking.replaceSeats(schedule, requestedSeats);
        booking.replacePassengerDetails(passengerDetailsFor(booking, booking.getPassenger().getUser(), requestedSeatNumbers, request.passengerDetails()));
        booking.setTotalAmount(schedule.getFare().multiply(BigDecimal.valueOf(requestedSeats.size())));
        try {
            Booking saved = bookings.saveAndFlush(booking);
            notifications.sendSeatChanged(saved, previousSeats, seatsLabel(saved));
            return DtoMapper.booking(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("One or more selected seats were just reserved by another booking");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> history(String email, Pageable pageable) {
        Passenger passenger = passengerByEmail(email);
        return PageResponse.from(bookings.findByPassengerIdOrderByBookingDateDesc(passenger.getId(), pageable).map(DtoMapper::booking));
    }

    @Override
    @Transactional(readOnly = true)
    public AvailableSeatsResponse availableSeats(Long scheduleId) {
        Schedule schedule = schedules.findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        List<SeatResponse> available = seats.findAvailableSeatsForSchedule(scheduleId).stream()
            .map(DtoMapper::seat)
            .toList();
        return new AvailableSeatsResponse(scheduleId, schedule.getBus().getCapacity(), available);
    }

    private void assertBookable(Schedule schedule) {
        if (!List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.DELAYED).contains(schedule.getStatus())) {
            throw new IllegalStateException("Schedule is unavailable for booking");
        }
        if (!schedule.getRoute().isActive()) {
            throw new IllegalStateException("Route is unavailable for booking");
        }
        if (schedule.getBus().getStatus() != BusStatus.ACTIVE) {
            throw new IllegalStateException("Bus is unavailable for booking");
        }
        if (schedule.getDepartureDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Departed schedules cannot be booked");
        }
    }

    private List<Integer> normalizedSeats(Set<Integer> seatNumbers, int capacity) {
        List<Integer> requestedSeats = seatNumbers.stream()
            .sorted()
            .toList();
        for (Integer seat : requestedSeats) {
            if (seat == null || seat < 1 || seat > capacity) {
                throw new IllegalArgumentException("Seat number " + seat + " is outside the bus capacity");
            }
        }
        return requestedSeats;
    }

    private List<Seat> seatsForSchedule(Schedule schedule, List<Integer> seatNumbers) {
        List<Seat> busSeats = seats.findByBusIdOrderBySeatNumber(schedule.getBus().getId());
        java.util.Map<Integer, Seat> seatsByNumber = busSeats.stream()
            .collect(java.util.stream.Collectors.toMap(Seat::getSeatNumber, seat -> seat));
        List<Seat> requestedSeats = seatNumbers.stream()
            .map(seatNumber -> {
                Seat seat = seatsByNumber.get(seatNumber);
                if (seat == null) {
                    throw new IllegalArgumentException("Seat number " + seatNumber + " does not exist for this bus");
                }
                return seat;
            })
            .toList();
        if (requestedSeats.size() != seatNumbers.size()) {
            throw new IllegalArgumentException("Invalid seat selection");
        }
        return requestedSeats;
    }

    private void assertSeatsAvailable(List<Seat> requestedSeats, Set<Long> occupiedSeatIds) {
        Set<Integer> conflicts = requestedSeats.stream()
            .filter(seat -> occupiedSeatIds.contains(seat.getId()))
            .map(Seat::getSeatNumber)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!conflicts.isEmpty()) {
            throw new ConflictException("Seats already reserved: " + conflicts);
        }
    }

    private List<BookingPassenger> passengerDetailsFor(Booking booking, User accountOwner, List<Integer> seatNumbers, List<PassengerDetailRequest> requestedDetails) {
        Map<Integer, PassengerDetailRequest> detailsBySeat = detailsBySeat(seatNumbers, requestedDetails);
        return seatNumbers.stream()
            .map(seatNumber -> passengerForSeat(booking, accountOwner, seatNumber, detailsBySeat.get(seatNumber)))
            .toList();
    }

    private Map<Integer, PassengerDetailRequest> detailsBySeat(List<Integer> seatNumbers, List<PassengerDetailRequest> requestedDetails) {
        Map<Integer, PassengerDetailRequest> detailsBySeat = new LinkedHashMap<>();
        if (requestedDetails == null || requestedDetails.isEmpty()) {
            return detailsBySeat;
        }

        boolean usesExplicitSeatNumbers = requestedDetails.stream().anyMatch(detail -> detail != null && detail.seatNumber() != null);
        if (!usesExplicitSeatNumbers) {
            if (requestedDetails.size() != seatNumbers.size()) {
                throw new IllegalArgumentException("Passenger details must match the selected seats");
            }
            for (int i = 0; i < seatNumbers.size(); i++) {
                detailsBySeat.put(seatNumbers.get(i), requestedDetails.get(i));
            }
            return detailsBySeat;
        }

        for (PassengerDetailRequest detail : requestedDetails) {
            if (detail == null || detail.seatNumber() == null) {
                throw new IllegalArgumentException("Passenger detail seat number is required when any passenger detail uses seat numbers");
            }
            if (!seatNumbers.contains(detail.seatNumber())) {
                throw new IllegalArgumentException("Passenger detail seat number " + detail.seatNumber() + " is not selected");
            }
            if (detailsBySeat.put(detail.seatNumber(), detail) != null) {
                throw new IllegalArgumentException("Duplicate passenger details for seat " + detail.seatNumber());
            }
        }
        return detailsBySeat;
    }

    private BookingPassenger passengerForSeat(Booking booking, User accountOwner, int seatNumber, PassengerDetailRequest detail) {
        if (detail == null || !isFriendPassenger(detail)) {
            return new BookingPassenger(booking, seatNumber, accountOwner.getFullName(), null, clean(accountOwner.getPhone()), clean(accountOwner.getEmail()));
        }

        String fullName = clean(detail.fullName());
        if (fullName == null) {
            throw new IllegalArgumentException("Passenger full name is required for seat " + seatNumber);
        }
        String email = clean(detail.email());
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Passenger email is invalid for seat " + seatNumber);
        }
        return new BookingPassenger(booking, seatNumber, fullName, clean(detail.nicOrPassport()), clean(detail.phone()), email);
    }

    private boolean isFriendPassenger(PassengerDetailRequest detail) {
        if (Boolean.TRUE.equals(detail.bookingForAnotherPassenger())) {
            return true;
        }
        if (Boolean.FALSE.equals(detail.bookingForAnotherPassenger())) {
            return false;
        }
        return clean(detail.fullName()) != null
            || clean(detail.nicOrPassport()) != null
            || clean(detail.phone()) != null
            || clean(detail.email()) != null;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String generateReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String reference = "SRL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!bookings.existsByBookingReference(reference)) {
                return reference;
            }
        }
        throw new IllegalStateException("Could not generate booking reference");
    }

    private Passenger passengerByEmail(String email) {
        User user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getPassenger() == null) {
            throw new ForbiddenActionException("Account is not a passenger");
        }
        return passengers.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Passenger profile not found"));
    }

    private Booking bookingById(Long id) {
        return bookings.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private void verifyOwnerOrStaff(Booking booking, String email, boolean staffAccess) {
        if (!staffAccess && !booking.getPassenger().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenActionException("Booking does not belong to user");
        }
    }

    private String seatsLabel(Booking booking) {
        return booking.getBookingSeats().stream()
            .map(bookingSeat -> String.valueOf(bookingSeat.getSeat().getSeatNumber()))
            .sorted()
            .reduce((left, right) -> left + ", " + right)
            .orElse("-");
    }
}
