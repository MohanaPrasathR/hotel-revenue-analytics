package com.mohana.staypulse.service;

import com.mohana.staypulse.dto.response.*;

import java.util.List;

/**
 * Read-only revenue and operational analytics over hotel bookings.
 * Cancelled bookings are excluded from every revenue figure unless stated otherwise.
 */
public interface AnalyticsService {

    /** Total revenue and count of all non-cancelled bookings. */
    TotalRevenueResponse getTotalRevenue();

    /** Revenue and booking count per hotel property, highest revenue first. */
    List<HotelRevenueResponse> getRevenueByHotel();

    /** Monthly revenue trend (by check-in month) with month-over-month growth. */
    List<MonthlyRevenueResponse> getRevenueByMonth();

    /** Number of bookings per status, including cancelled bookings. */
    List<StatusCountResponse> getBookingCountByStatus();

    /** Average revenue per non-cancelled booking. */
    AverageRevenueResponse getAverageRevenue();

    /**
     * Top hotels ranked by revenue.
     *
     * @param limit number of hotels to return; non-positive values default to 5 and values above 50 are capped
     */
    List<TopHotelResponse> getTopHotels(int limit);

    /** Revenue, average revenue and share of total per room type. */
    List<RoomTypeRevenueResponse> getRevenueByRoomType();

    /** Operational KPIs: ADR, average length of stay, room nights and cancellation rate. */
    OperationalMetricsResponse getOperationalMetrics();

    /** Compact executive summary combining the headline revenue and operational metrics. */
    RevenueSummaryDigestResponse getRevenueSummaryDigest();

    /** Exports every booking as UTF-8 CSV with spreadsheet-formula protection on free-text fields. */
    byte[] exportBookingsCsv();
}
