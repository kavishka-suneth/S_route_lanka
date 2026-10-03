package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.model.*;
import lk.smartroute.repository.*;
import lk.smartroute.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {
    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final UserRepository users;
    private final StaffRepository staffRepository;
    private final ScheduleRepository schedules;

    public ReportServiceImpl(
        PaymentRepository payments,
        BookingRepository bookings,
        BookingSeatRepository bookingSeats,
        UserRepository users,
        StaffRepository staffRepository,
        ScheduleRepository schedules
    ) {
        this.payments = payments;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.users = users;
        this.staffRepository = staffRepository;
        this.schedules = schedules;
    }

    @Override
    public RevenueReport totalRevenue() {
        return new RevenueReport(
            payments.totalSuccessfulRevenue(),
            payments.countByPaymentStatus(PaymentStatus.SUCCESS),
            bookings.count()
        );
    }

    @Override
    public RevenueReport revenueBetween(Instant from, Instant to) {
        validateRange(from, to);
        LocalDateTime startDate = localDateTime(from);
        LocalDateTime endDate = localDateTime(to);
        return new RevenueReport(
            payments.revenueBetween(startDate, endDate),
            payments.countByPaymentStatusAndPaymentDateBetween(PaymentStatus.SUCCESS, startDate, endDate),
            bookings.count()
        );
    }

    @Override
    public RevenueDetailsReport revenueDetails(Instant from, Instant to) {
        validateRange(from, to);
        LocalDateTime startDate = localDateTimeOrNull(from);
        LocalDateTime endDate = localDateTimeOrNull(to);
        return new RevenueDetailsReport(
            money(payments.sumAmountByStatusBetween(PaymentStatus.SUCCESS, startDate, endDate)),
            payments.countByStatusBetween(PaymentStatus.SUCCESS, startDate, endDate),
            money(payments.sumAmountByStatusBetween(PaymentStatus.REFUNDED, startDate, endDate)),
            payments.countByStatusBetween(PaymentStatus.REFUNDED, startDate, endDate),
            paymentMethods(startDate, endDate)
        );
    }

    @Override
    public BookingCountReport bookingCount() {
        return new BookingCountReport(
            bookings.count(),
            bookings.countByBookingStatus(BookingStatus.PENDING_PAYMENT),
            bookings.countByBookingStatus(BookingStatus.CONFIRMED),
            bookings.countByBookingStatus(BookingStatus.CANCELLED),
            bookings.countByBookingStatus(BookingStatus.COMPLETED)
        );
    }

    @Override
    public TicketSalesReport ticketSales() {
        return new TicketSalesReport(bookingSeats.count());
    }

    @Override
    public PageResponse<ScheduleOccupancyReport> scheduleOccupancy(LocalDate from, LocalDate to, String route, LocalDate travelDate, Long routeId, Long scheduleId, Pageable pageable) {
        validateRange(from, to);
        return PageResponse.from(schedules.occupancyDetails(from, to, normalize(route), travelDate, routeId, scheduleId, pageable).map(row -> new ScheduleOccupancyReport(
            row.getScheduleId(),
            row.getRoute(),
            row.getTravelDate(),
            row.getDepartureTime(),
            row.getBusCapacity(),
            row.getOccupiedSeats(),
            percentage(row.getOccupiedSeats(), row.getBusCapacity()).doubleValue()
        )));
    }

    @Override
    public TicketSalesDetailsReport ticketSalesDetails(LocalDate from, LocalDate to, Long routeId, Long scheduleId, Pageable pageable) {
        validateRange(from, to);
        Page<TicketSalesScheduleReport> page = schedules.ticketSalesDetails(from, to, routeId, scheduleId, pageable).map(this::ticketSalesRow);
        return new TicketSalesDetailsReport(ticketSalesSummary(schedules.ticketSalesDetailsList(from, to, routeId, scheduleId)), PageResponse.from(page));
    }

    @Override
    public OccupancyDetailsReport occupancyDetails(LocalDate from, LocalDate to, String route, LocalDate travelDate, Long routeId, Long scheduleId, Pageable pageable) {
        validateRange(from, to);
        String routeFilter = normalize(route);
        Page<ScheduleOccupancyDetailReport> page = schedules.occupancyDetails(from, to, routeFilter, travelDate, routeId, scheduleId, pageable).map(this::occupancyRow);
        List<ScheduleOccupancyDetailReport> rows = schedules.occupancyDetailsList(from, to, routeFilter, travelDate, routeId, scheduleId).stream().map(this::occupancyRow).toList();
        return new OccupancyDetailsReport(occupancySummary(rows), PageResponse.from(page));
    }

    @Override
    public ManagementReport managementReport(Instant from, Instant to) {
        validateRange(from, to);
        LocalDateTime startDateTime = localDateTimeOrNull(from);
        LocalDateTime endDateTime = localDateTimeOrNull(to);
        LocalDate startDate = localDateOrNull(from);
        LocalDate endDate = localDateOrNull(to);
        List<ScheduleRepository.TicketSalesScheduleProjection> salesRows = schedules.ticketSalesDetailsList(startDate, endDate, null, null);
        List<ScheduleOccupancyDetailReport> occupancyRows = schedules.occupancyDetailsList(startDate, endDate, null, null, null, null).stream().map(this::occupancyRow).toList();
        TicketSalesSummaryReport sales = ticketSalesSummary(salesRows);
        OccupancySummaryReport occupancy = occupancySummary(occupancyRows);
        return new ManagementReport(
            bookings.countByStatusAndBookingDateBetween(null, startDateTime, endDateTime),
            bookings.countByStatusAndBookingDateBetween(BookingStatus.PENDING_PAYMENT, startDateTime, endDateTime),
            bookings.countByStatusAndBookingDateBetween(BookingStatus.CONFIRMED, startDateTime, endDateTime),
            bookings.countByStatusAndBookingDateBetween(BookingStatus.COMPLETED, startDateTime, endDateTime),
            bookings.countByStatusAndBookingDateBetween(BookingStatus.CANCELLED, startDateTime, endDateTime),
            sales.totalTicketsSold(),
            occupancy.schedules(),
            occupancy.averageOccupancyPercent(),
            money(payments.sumAmountByStatusBetween(PaymentStatus.SUCCESS, startDateTime, endDateTime)),
            payments.countByStatusBetween(PaymentStatus.SUCCESS, startDateTime, endDateTime),
            bookings.countByStatusAndBookingDateBetween(BookingStatus.CANCELLED, startDateTime, endDateTime),
            routePerformance(salesRows)
        );
    }

    @Override
    public FinancialReport financialReport(Instant from, Instant to) {
        validateRange(from, to);
        LocalDateTime startDate = localDateTimeOrNull(from);
        LocalDateTime endDate = localDateTimeOrNull(to);
        return new FinancialReport(
            money(payments.sumAmountByStatusBetween(PaymentStatus.SUCCESS, startDate, endDate)),
            payments.countByStatusBetween(PaymentStatus.SUCCESS, startDate, endDate),
            payments.countByStatusBetween(PaymentStatus.PENDING, startDate, endDate),
            payments.countByStatusBetween(PaymentStatus.FAILED, startDate, endDate),
            payments.countByStatusBetween(PaymentStatus.REFUNDED, startDate, endDate),
            money(payments.sumAmountByStatusBetween(PaymentStatus.REFUNDED, startDate, endDate)),
            paymentMethods(startDate, endDate)
        );
    }

    @Override
    public CancelledBookingsReport cancelledBookings() {
        return new CancelledBookingsReport(bookings.countByBookingStatus(BookingStatus.CANCELLED));
    }

    @Override
    public UserStatisticsReport userStatistics() {
        return new UserStatisticsReport(
            users.count(),
            users.countByActiveTrue(),
            users.countByUserType(UserType.PASSENGER),
            users.countByUserType(UserType.STAFF),
            staffRepository.countByStaffType(StaffType.CUSTOMER_SERVICE_STAFF),
            staffRepository.countByStaffType(StaffType.TRANSPORT_COORDINATOR),
            staffRepository.countByStaffType(StaffType.FINANCE_OFFICER),
            staffRepository.countByStaffType(StaffType.OPERATIONS_MANAGER),
            staffRepository.countByStaffType(StaffType.SYSTEM_ADMINISTRATOR),
            staffRepository.countByStaffType(StaffType.IT_SUPPORT_STAFF)
        );
    }

    @Override
    public PaymentStatisticsReport paymentStatistics() {
        return new PaymentStatisticsReport(
            payments.countByPaymentStatus(PaymentStatus.PENDING),
            payments.countByPaymentStatus(PaymentStatus.SUCCESS),
            payments.countByPaymentStatus(PaymentStatus.FAILED),
            payments.countByPaymentStatus(PaymentStatus.REFUNDED)
        );
    }

    @Override
    public String summaryCsv() {
        RevenueReport revenue = totalRevenue();
        BookingCountReport bookingCount = bookingCount();
        TicketSalesReport ticketSales = ticketSales();
        CancelledBookingsReport cancelledBookings = cancelledBookings();
        UserStatisticsReport userStatistics = userStatistics();
        PaymentStatisticsReport paymentStatistics = paymentStatistics();

        StringBuilder csv = new StringBuilder("section,metric,value\n");
        append(csv, "Revenue", "Total revenue", revenue.totalRevenue());
        append(csv, "Revenue", "Successful payments", revenue.successfulPayments());
        append(csv, "Bookings", "Total bookings", bookingCount.totalBookings());
        append(csv, "Bookings", "Pending payment", bookingCount.pendingPayment());
        append(csv, "Bookings", "Confirmed", bookingCount.confirmed());
        append(csv, "Bookings", "Cancelled", cancelledBookings.cancelledBookings());
        append(csv, "Bookings", "Completed", bookingCount.completed());
        append(csv, "Bookings", "Reserved seats", ticketSales.activeReservedSeats());
        append(csv, "Users", "Total users", userStatistics.totalUsers());
        append(csv, "Users", "Active users", userStatistics.activeUsers());
        append(csv, "Users", "Passengers", userStatistics.passengers());
        append(csv, "Users", "Staff", userStatistics.staff());
        append(csv, "Payments", "Pending", paymentStatistics.pending());
        append(csv, "Payments", "Successful", paymentStatistics.successful());
        append(csv, "Payments", "Failed", paymentStatistics.failed());
        append(csv, "Payments", "Refunded", paymentStatistics.refunded());
        return csv.toString();
    }

    @Override
    public String ticketSalesCsv(LocalDate from, LocalDate to, Long routeId, Long scheduleId) {
        validateRange(from, to);
        StringBuilder csv = new StringBuilder("scheduleId,route,travelDate,departureTime,bus,busCapacity,ticketsSold,remainingSeats,occupancyPercent,bookings,ticketSalesAmount\n");
        schedules.ticketSalesDetailsList(from, to, routeId, scheduleId).stream().map(this::ticketSalesRow).forEach(row -> csv
            .append(row.scheduleId()).append(',')
            .append(escape(row.route())).append(',')
            .append(escape(row.travelDate())).append(',')
            .append(escape(row.departureTime())).append(',')
            .append(escape(row.busRegistrationNo())).append(',')
            .append(row.busCapacity()).append(',')
            .append(row.ticketsSold()).append(',')
            .append(row.remainingSeats()).append(',')
            .append(row.occupancyPercent()).append(',')
            .append(row.bookingCount()).append(',')
            .append(row.ticketSalesAmount()).append('\n'));
        return csv.toString();
    }

    @Override
    public String managementCsv(Instant from, Instant to) {
        ManagementReport report = managementReport(from, to);
        StringBuilder csv = new StringBuilder("section,metric,value\n");
        append(csv, "Bookings", "Total bookings", report.totalBookings());
        append(csv, "Bookings", "Pending payment bookings", report.pendingPaymentBookings());
        append(csv, "Bookings", "Confirmed bookings", report.confirmedBookings());
        append(csv, "Bookings", "Completed bookings", report.completedBookings());
        append(csv, "Bookings", "Cancelled bookings", report.cancelledBookings());
        append(csv, "Ticket sales", "Total tickets sold", report.totalTicketsSold());
        append(csv, "Operations", "Total schedules", report.totalSchedules());
        append(csv, "Operations", "Average occupancy percent", report.averageOccupancyPercent());
        append(csv, "Revenue", "Successful revenue", report.successfulRevenue());
        append(csv, "Revenue", "Successful payment count", report.successfulPaymentCount());
        report.routePerformance().forEach(route -> {
            append(csv, "Route performance", route.route() + " bookings", route.bookings());
            append(csv, "Route performance", route.route() + " tickets sold", route.ticketsSold());
            append(csv, "Route performance", route.route() + " revenue", route.revenue());
            append(csv, "Route performance", route.route() + " average occupancy percent", route.averageOccupancyPercent());
        });
        return csv.toString();
    }

    @Override
    public String financialCsv(Instant from, Instant to) {
        FinancialReport report = financialReport(from, to);
        StringBuilder csv = new StringBuilder("section,metric,value\n");
        append(csv, "Financial", "Successful revenue", report.successfulRevenue());
        append(csv, "Financial", "Successful payment count", report.successfulPaymentCount());
        append(csv, "Financial", "Pending payment count", report.pendingPaymentCount());
        append(csv, "Financial", "Failed payment count", report.failedPaymentCount());
        append(csv, "Financial", "Refunded payment count", report.refundedPaymentCount());
        append(csv, "Financial", "Refunded amount", report.refundedAmount());
        report.revenueByPaymentMethod().forEach(method -> {
            append(csv, "Revenue by payment method", method.paymentMethod() + " revenue", method.revenue());
            append(csv, "Revenue by payment method", method.paymentMethod() + " successful payments", method.successfulPayments());
        });
        return csv.toString();
    }

    private TicketSalesScheduleReport ticketSalesRow(ScheduleRepository.TicketSalesScheduleProjection row) {
        long sold = row.getTicketsSold();
        int capacity = row.getBusCapacity();
        return new TicketSalesScheduleReport(
            row.getScheduleId(),
            row.getRoute(),
            row.getTravelDate(),
            row.getDepartureTime(),
            row.getBusRegistrationNo(),
            capacity,
            sold,
            Math.max(0, capacity - sold),
            percentage(sold, capacity).doubleValue(),
            row.getBookingCount(),
            money(row.getTicketSalesAmount())
        );
    }

    private ScheduleOccupancyDetailReport occupancyRow(ScheduleRepository.ScheduleOccupancyProjection row) {
        long occupied = row.getOccupiedSeats();
        int capacity = row.getBusCapacity();
        return new ScheduleOccupancyDetailReport(
            row.getScheduleId(),
            row.getRoute(),
            row.getTravelDate(),
            row.getDepartureTime(),
            row.getBusRegistrationNo(),
            capacity,
            occupied,
            Math.max(0, capacity - occupied),
            percentage(occupied, capacity).doubleValue()
        );
    }

    private TicketSalesSummaryReport ticketSalesSummary(List<ScheduleRepository.TicketSalesScheduleProjection> rows) {
        long tickets = rows.stream().mapToLong(ScheduleRepository.TicketSalesScheduleProjection::getTicketsSold).sum();
        long bookingCount = rows.stream().mapToLong(ScheduleRepository.TicketSalesScheduleProjection::getBookingCount).sum();
        BigDecimal amount = rows.stream()
            .map(ScheduleRepository.TicketSalesScheduleProjection::getTicketSalesAmount)
            .map(this::money)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TicketSalesSummaryReport(tickets, bookingCount, amount);
    }

    private OccupancySummaryReport occupancySummary(List<ScheduleOccupancyDetailReport> rows) {
        double average = rows.isEmpty()
            ? 0.0
            : rows.stream().mapToDouble(ScheduleOccupancyDetailReport::occupancyPercent).average().orElse(0.0);
        return new OccupancySummaryReport(rows.size(), BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    private List<PaymentMethodRevenueReport> paymentMethods(LocalDateTime from, LocalDateTime to) {
        return payments.revenueGroupedByPaymentMethodBetween(from, to).stream()
            .map(row -> new PaymentMethodRevenueReport(row.getPaymentMethod(), money(row.getRevenue()), row.getSuccessfulPayments()))
            .toList();
    }

    private List<ManagementRoutePerformanceReport> routePerformance(List<ScheduleRepository.TicketSalesScheduleProjection> rows) {
        Map<Long, RouteAccumulator> routes = new LinkedHashMap<>();
        rows.forEach(row -> {
            RouteAccumulator acc = routes.computeIfAbsent(row.getRouteId(), id -> new RouteAccumulator(row.getRoute()));
            long sold = row.getTicketsSold();
            acc.bookings += row.getBookingCount();
            acc.tickets += sold;
            acc.revenue = acc.revenue.add(money(row.getTicketSalesAmount()));
            acc.occupancyTotal = acc.occupancyTotal.add(percentage(sold, row.getBusCapacity()));
            acc.schedules++;
        });
        return routes.entrySet().stream()
            .map(entry -> {
                RouteAccumulator acc = entry.getValue();
                BigDecimal average = acc.schedules == 0
                    ? BigDecimal.ZERO
                    : acc.occupancyTotal.divide(BigDecimal.valueOf(acc.schedules), 2, RoundingMode.HALF_UP);
                return new ManagementRoutePerformanceReport(entry.getKey(), acc.route, acc.bookings, acc.tickets, acc.revenue, average.doubleValue());
            })
            .toList();
    }

    private BigDecimal percentage(long count, int capacity) {
        if (capacity <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime localDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private LocalDateTime localDateTimeOrNull(Instant instant) {
        return instant == null ? null : localDateTime(instant);
    }

    private LocalDate localDateOrNull(Instant instant) {
        return instant == null ? null : localDateTime(instant).toLocalDate();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }

    private void append(StringBuilder csv, String section, String metric, Object value) {
        csv.append(escape(section))
            .append(',')
            .append(escape(metric))
            .append(',')
            .append(escape(value))
            .append('\n');
    }

    private String escape(Object value) {
        String text = String.valueOf(value == null ? "" : value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private static final class RouteAccumulator {
        private final String route;
        private long bookings;
        private long tickets;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal occupancyTotal = BigDecimal.ZERO;
        private long schedules;

        private RouteAccumulator(String route) {
            this.route = route;
        }
    }
}
