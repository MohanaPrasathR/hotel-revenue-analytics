package com.mohana.staypulse.service;

import com.mohana.staypulse.dto.response.*;

import java.util.List;

public interface AnalyticsService {

    TotalRevenueResponse getTotalRevenue();

    List<HotelRevenueResponse> getRevenueByHotel();

    List<MonthlyRevenueResponse> getRevenueByMonth();

    List<StatusCountResponse> getBookingCountByStatus();

    AverageRevenueResponse getAverageRevenue();

    List<TopHotelResponse> getTopHotels(int limit);

    List<RoomTypeRevenueResponse> getRevenueByRoomType();

    OperationalMetricsResponse getOperationalMetrics();

    RevenueSummaryDigestResponse getRevenueSummaryDigest();

    byte[] exportBookingsCsv();
}
