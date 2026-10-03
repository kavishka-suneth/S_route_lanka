package lk.smartroute.controller;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.service.ReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('FINANCE_OFFICER','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/revenue/total")
    public RevenueReport totalRevenue() {
        return reports.totalRevenue();
    }

    @GetMapping("/revenue")
    public RevenueReport revenueBetween(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return reports.revenueBetween(from, to);
    }

    @GetMapping("/revenue/details")
    public RevenueDetailsReport revenueDetails(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return reports.revenueDetails(from, to);
    }

    @GetMapping("/bookings/count")
    public BookingCountReport bookingCount() {
        return reports.bookingCount();
    }

    @GetMapping("/ticket-sales")
    public TicketSalesReport ticketSales() {
        return reports.ticketSales();
    }

    @GetMapping("/schedule-occupancy")
    public PageResponse<ScheduleOccupancyReport> scheduleOccupancy(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String route,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
        @RequestParam(required = false) Long routeId,
        @RequestParam(required = false) Long scheduleId,
        @PageableDefault(sort = "departureTime") Pageable pageable
    ) {
        return reports.scheduleOccupancy(from, to, route, travelDate, routeId, scheduleId, pageable);
    }

    @GetMapping("/ticket-sales/details")
    public TicketSalesDetailsReport ticketSalesDetails(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long routeId,
        @RequestParam(required = false) Long scheduleId,
        @PageableDefault(sort = "travelDate") Pageable pageable
    ) {
        return reports.ticketSalesDetails(from, to, routeId, scheduleId, pageable);
    }

    @GetMapping("/schedule-occupancy/details")
    public OccupancyDetailsReport occupancyDetails(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String route,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
        @RequestParam(required = false) Long routeId,
        @RequestParam(required = false) Long scheduleId,
        @PageableDefault(sort = "travelDate") Pageable pageable
    ) {
        return reports.occupancyDetails(from, to, route, travelDate, routeId, scheduleId, pageable);
    }

    @GetMapping("/management")
    public ManagementReport managementReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return reports.managementReport(from, to);
    }

    @GetMapping("/financial")
    public FinancialReport financialReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return reports.financialReport(from, to);
    }

    @GetMapping("/cancelled-bookings")
    public CancelledBookingsReport cancelledBookings() {
        return reports.cancelledBookings();
    }

    @GetMapping("/users")
    public UserStatisticsReport userStatistics() {
        return reports.userStatistics();
    }

    @GetMapping("/payments")
    public PaymentStatisticsReport paymentStatistics() {
        return reports.paymentStatistics();
    }

    @GetMapping(value = "/export/summary.csv", produces = "text/csv")
    public ResponseEntity<String> summaryCsv() {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"smartroute-report-summary.csv\"")
            .body(reports.summaryCsv());
    }

    @GetMapping(value = "/export/ticket-sales.csv", produces = "text/csv")
    public ResponseEntity<String> ticketSalesCsv(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long routeId,
        @RequestParam(required = false) Long scheduleId
    ) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"smartroute-ticket-sales.csv\"")
            .body(reports.ticketSalesCsv(from, to, routeId, scheduleId));
    }

    @GetMapping(value = "/export/management.csv", produces = "text/csv")
    public ResponseEntity<String> managementCsv(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"smartroute-management-report.csv\"")
            .body(reports.managementCsv(from, to));
    }

    @GetMapping(value = "/export/financial.csv", produces = "text/csv")
    public ResponseEntity<String> financialCsv(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"smartroute-financial-report.csv\"")
            .body(reports.financialCsv(from, to));
    }
}
