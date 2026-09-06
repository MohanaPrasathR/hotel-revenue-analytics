package com.mohana.hotelanalytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Monthly revenue aggregation with period-over-period growth differential")
public class MonthlyRevenueResponse {

    @Schema(description = "Calendar Year and Month in YYYY-MM format", example = "2026-09")
    private String yearMonth;

    @Schema(description = "Gross realized revenue in this calendar month", example = "18250.00")
    private BigDecimal totalRevenue;

    @Schema(description = "Total non-cancelled reservations in this month", example = "14")
    private Long bookingCount;

    @Schema(description = "Gross revenue realized in preceding monthly period", example = "15000.00")
    private BigDecimal previousMonthRevenue;

    @Schema(description = "Month-over-Month (MoM) revenue growth percentage", example = "21.7")
    private Double growthRatePercentage;
}

