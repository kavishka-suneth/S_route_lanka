package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.*;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.time.LocalDate;

public interface ReportService {
    RevenueReport totalRevenue();

    RevenueReport revenueBetween(Instant from, Instant to);

    RevenueDetailsReport revenueDetails(Instant from, Instant to);

    BookingCountReport bookingCount();

    TicketSalesReport ticketSales();

    PageResponse<ScheduleOccupancyReport> scheduleOccupancy(LocalDate from, LocalDate to, String route, LocalDate travelDate, Long routeId, Long scheduleId, Pageable pageable);

    TicketSalesDetailsReport ticketSalesDetails(LocalDate from, LocalDate to, Long routeId, Long scheduleId, Pageable pageable);

    OccupancyDetailsReport occupancyDetails(LocalDate from, LocalDate to, String route, LocalDate travelDate, Long routeId, Long scheduleId, Pageable pageable);

    ManagementReport managementReport(Instant from, Instant to);

    FinancialReport financialReport(Instant from, Instant to);

    CancelledBookingsReport cancelledBookings();

    UserStatisticsReport userStatistics();

    PaymentStatisticsReport paymentStatistics();

    String summaryCsv();

    String ticketSalesCsv(LocalDate from, LocalDate to, Long routeId, Long scheduleId);

    String managementCsv(Instant from, Instant to);

    String financialCsv(Instant from, Instant to);
}
