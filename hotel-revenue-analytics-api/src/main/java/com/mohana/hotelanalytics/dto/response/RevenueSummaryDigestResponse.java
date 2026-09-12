package com.mohana.hotelanalytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Executive high-level digest response combining gross figures, top yield property,
 * and key operational performance ratios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueSummaryDigestResponse {

    private BigDecimal totalRevenue;
    private long totalActiveBookings;
    private BigDecimal averageDailyRate;
    private double cancellationRate;
    private String topPerformingHotel;
    private BigDecimal topHotelRevenue;
    private String mostPopularRoomType;
    private long totalRoomNights;
}
