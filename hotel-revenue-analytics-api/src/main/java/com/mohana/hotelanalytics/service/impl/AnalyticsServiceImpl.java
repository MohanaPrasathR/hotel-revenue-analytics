package com.mohana.hotelanalytics.service.impl;

import com.mohana.hotelanalytics.dto.response.*;
import com.mohana.hotelanalytics.entity.BookingStatus;
import com.mohana.hotelanalytics.repository.BookingRepository;
import com.mohana.hotelanalytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "revenueAnalytics", key = "'totalRevenue'")
    public TotalRevenueResponse getTotalRevenue() {
        BigDecimal totalRevenue = bookingRepository.findTotalRevenueOfActiveBookings();
        Long count = bookingRepository.countActiveBookings();

        return TotalRevenueResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .eligibleBookingsCount(count != null ? count : 0L)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelRevenueResponse> getRevenueByHotel() {
        List<Object[]> results = bookingRepository.findRevenueGroupedByHotel();

        return results.stream().map(row -> HotelRevenueResponse.builder()
                .hotelName((String) row[0])
                .totalRevenue((BigDecimal) row[1])
                .bookingCount((Long) row[2])
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyRevenueResponse> getRevenueByMonth() {
        List<Object[]> results = bookingRepository.findMonthlyRevenueTrend();

        List<MonthlyRevenueResponse> monthlyResponses = new ArrayList<>();
        BigDecimal previousMonthRevenue = null;

        for (Object[] row : results) {
            String yearMonth = (String) row[0];
            BigDecimal totalRevenue = (BigDecimal) row[1];
            Long count = (Long) row[2];

            Double growthRate = null;
            if (previousMonthRevenue != null && previousMonthRevenue.compareTo(BigDecimal.ZERO) > 0 && totalRevenue != null) {
                double diff = totalRevenue.subtract(previousMonthRevenue).doubleValue();
                growthRate = Math.round((diff / previousMonthRevenue.doubleValue()) * 1000.0) / 10.0;
            }

            monthlyResponses.add(MonthlyRevenueResponse.builder()
                    .yearMonth(yearMonth)
                    .totalRevenue(totalRevenue)
                    .bookingCount(count)
                    .previousMonthRevenue(previousMonthRevenue)
                    .growthRatePercentage(growthRate)
                    .build());

            previousMonthRevenue = totalRevenue;
        }

        return monthlyResponses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusCountResponse> getBookingCountByStatus() {
        List<Object[]> results = bookingRepository.findBookingCountGroupedByStatus();

        return results.stream().map(row -> StatusCountResponse.builder()
                .bookingStatus((BookingStatus) row[0])
                .count((Long) row[1])
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AverageRevenueResponse getAverageRevenue() {
        Double avg = bookingRepository.findAverageRevenuePerBooking();
        Long count = bookingRepository.countActiveBookings();

        BigDecimal averageRevenue = avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return AverageRevenueResponse.builder()
                .averageRevenuePerBooking(averageRevenue)
                .totalBookingsEvaluated(count != null ? count : 0L)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopHotelResponse> getTopHotels(int limit) {
        int targetLimit = limit > 0 ? limit : 5;
        List<Object[]> results = bookingRepository.findTopHotelsByRevenue(PageRequest.of(0, targetLimit));

        List<TopHotelResponse> topHotels = new ArrayList<>();
        int rank = 1;

        for (Object[] row : results) {
            topHotels.add(TopHotelResponse.builder()
                    .rank(rank++)
                    .hotelName((String) row[0])
                    .totalRevenue((BigDecimal) row[1])
                    .totalBookings((Long) row[2])
                    .build());
        }

        return topHotels;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeRevenueResponse> getRevenueByRoomType() {
        List<Object[]> results = bookingRepository.findRevenueGroupedByRoomType();
        BigDecimal grandTotal = bookingRepository.findTotalRevenueOfActiveBookings();
        double totalRevDouble = grandTotal != null ? grandTotal.doubleValue() : 0.0;

        List<RoomTypeRevenueResponse> responses = new ArrayList<>();
        for (Object[] row : results) {
            com.mohana.hotelanalytics.entity.RoomType roomType = (com.mohana.hotelanalytics.entity.RoomType) row[0];
            BigDecimal totalRev = (BigDecimal) row[1];
            Long count = (Long) row[2];

            BigDecimal avgRev = (count != null && count > 0 && totalRev != null)
                    ? totalRev.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            double pct = (totalRevDouble > 0 && totalRev != null)
                    ? Math.round((totalRev.doubleValue() / totalRevDouble) * 1000.0) / 10.0
                    : 0.0;

            responses.add(RoomTypeRevenueResponse.builder()
                    .roomType(roomType)
                    .totalRevenue(totalRev)
                    .bookingCount(count)
                    .averageRevenue(avgRev)
                    .revenuePercentage(pct)
                    .build());
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public OperationalMetricsResponse getOperationalMetrics() {
        List<com.mohana.hotelanalytics.entity.Booking> allBookings = bookingRepository.findAll();
        long totalBookings = allBookings.size();
        
        List<com.mohana.hotelanalytics.entity.Booking> activeBookingsList = allBookings.stream()
                .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                .collect(Collectors.toList());

        long activeCount = activeBookingsList.size();
        long cancelledCount = totalBookings - activeCount;

        long totalNights = activeBookingsList.stream()
                .mapToLong(b -> java.time.temporal.ChronoUnit.DAYS.between(b.getCheckInDate(), b.getCheckOutDate()))
                .filter(nights -> nights > 0)
                .sum();

        BigDecimal totalRevenue = activeBookingsList.stream()
                .map(com.mohana.hotelanalytics.entity.Booking::getTotalRevenue)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal adr = (totalNights > 0)
                ? totalRevenue.divide(BigDecimal.valueOf(totalNights), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double alos = (activeCount > 0)
                ? Math.round(((double) totalNights / activeCount) * 10.0) / 10.0
                : 0.0;

        double cancelRate = (totalBookings > 0)
                ? Math.round(((double) cancelledCount / totalBookings) * 1000.0) / 10.0
                : 0.0;

        return OperationalMetricsResponse.builder()
                .averageDailyRate(adr)
                .averageLengthOfStay(alos)
                .totalRoomNights(totalNights)
                .cancellationRate(cancelRate)
                .totalBookings(totalBookings)
                .activeBookings(activeCount)
                .cancelledBookings(cancelledCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueSummaryDigestResponse getRevenueSummaryDigest() {
        TotalRevenueResponse totalRevenueResp = getTotalRevenue();
        OperationalMetricsResponse opsMetrics = getOperationalMetrics();
        List<TopHotelResponse> topHotels = getTopHotels(1);
        List<RoomTypeRevenueResponse> roomTypes = getRevenueByRoomType();

        String topHotel = topHotels.isEmpty() ? "N/A" : topHotels.get(0).getHotelName();
        BigDecimal topHotelRev = topHotels.isEmpty() ? BigDecimal.ZERO : topHotels.get(0).getTotalRevenue();
        String popularRoomType = roomTypes.isEmpty() ? "N/A" : roomTypes.get(0).getRoomType();

        return RevenueSummaryDigestResponse.builder()
                .totalRevenue(totalRevenueResp.getTotalRevenue())
                .totalActiveBookings(totalRevenueResp.getEligibleBookingsCount())
                .averageDailyRate(opsMetrics.getAverageDailyRate())
                .cancellationRate(opsMetrics.getCancellationRate())
                .topPerformingHotel(topHotel)
                .topHotelRevenue(topHotelRev)
                .mostPopularRoomType(popularRoomType)
                .totalRoomNights(opsMetrics.getTotalRoomNights())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportBookingsCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Hotel Name,Guest Name,Check-In Date,Check-Out Date,Guests,Room Type,Status,Total Revenue ($),Created At\n");
        bookingRepository.findAll().forEach(b -> {
            csv.append(b.getId()).append(",")
               .append("\"").append(b.getHotelName().replace("\"", "\"\"")).append("\",")
               .append("\"").append(b.getGuestName().replace("\"", "\"\"")).append("\",")
               .append(b.getCheckInDate()).append(",")
               .append(b.getCheckOutDate()).append(",")
               .append(b.getGuests()).append(",")
               .append(b.getRoomType()).append(",")
               .append(b.getBookingStatus()).append(",")
               .append(b.getTotalRevenue()).append(",")
               .append(b.getCreatedAt()).append("\n");
        });
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
