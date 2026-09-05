package com.mohana.hotelanalytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hospitality industry standard operational KPI metrics")
public class OperationalMetricsResponse {

    @Schema(description = "Average Daily Rate (ADR) across all active room nights", example = "215.50")
    private BigDecimal averageDailyRate;

    @Schema(description = "Average Length of Stay (ALOS) in nights per booking", example = "3.4")
    private Double averageLengthOfStay;

    @Schema(description = "Total cumulative room nights occupied/reserved", example = "185")
    private Long totalRoomNights;

    @Schema(description = "Booking cancellation percentage rate", example = "8.3")
    private Double cancellationRate;

    @Schema(description = "Total registered bookings count", example = "24")
    private Long totalBookings;

    @Schema(description = "Count of active non-cancelled bookings", example = "22")
    private Long activeBookings;

    @Schema(description = "Count of cancelled bookings", example = "2")
    private Long cancelledBookings;
}
