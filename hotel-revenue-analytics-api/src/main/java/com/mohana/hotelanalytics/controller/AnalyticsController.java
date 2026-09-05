package com.mohana.hotelanalytics.controller;

import com.mohana.hotelanalytics.dto.response.*;
import com.mohana.hotelanalytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/total-revenue")
    public ResponseEntity<TotalRevenueResponse> getTotalRevenue() {
        TotalRevenueResponse response = analyticsService.getTotalRevenue();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue-by-hotel")
    public ResponseEntity<List<HotelRevenueResponse>> getRevenueByHotel() {
        List<HotelRevenueResponse> response = analyticsService.getRevenueByHotel();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue-by-month")
    public ResponseEntity<List<MonthlyRevenueResponse>> getRevenueByMonth() {
        List<MonthlyRevenueResponse> response = analyticsService.getRevenueByMonth();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/booking-count-by-status")
    public ResponseEntity<List<StatusCountResponse>> getBookingCountByStatus() {
        List<StatusCountResponse> response = analyticsService.getBookingCountByStatus();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue-by-room-type")
    public ResponseEntity<List<RoomTypeRevenueResponse>> getRevenueByRoomType() {
        List<RoomTypeRevenueResponse> response = analyticsService.getRevenueByRoomType();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operational-metrics")
    public ResponseEntity<OperationalMetricsResponse> getOperationalMetrics() {
        OperationalMetricsResponse response = analyticsService.getOperationalMetrics();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/average-revenue")
    public ResponseEntity<AverageRevenueResponse> getAverageRevenue() {
        AverageRevenueResponse response = analyticsService.getAverageRevenue();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-hotels")
    public ResponseEntity<List<TopHotelResponse>> getTopHotels(
            @RequestParam(defaultValue = "5") int limit) {
        List<TopHotelResponse> response = analyticsService.getTopHotels(limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportBookingsCsv() {
        byte[] csvData = analyticsService.exportBookingsCsv();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hotel_bookings_export.csv")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csvData);
    }
}
