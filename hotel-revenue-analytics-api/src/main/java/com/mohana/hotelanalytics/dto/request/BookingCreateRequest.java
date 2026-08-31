package com.mohana.hotelanalytics.dto.request;

import com.mohana.hotelanalytics.entity.BookingStatus;
import com.mohana.hotelanalytics.entity.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for creating a new hotel reservation")
public class BookingCreateRequest {

    @NotBlank(message = "Hotel name is required")
    @Size(max = 100, message = "Hotel name cannot exceed 100 characters")
    @Schema(description = "Hotel property name", example = "Grand Horizon Resort")
    private String hotelName;

    @NotBlank(message = "Guest name is required")
    @Size(max = 100, message = "Guest name cannot exceed 100 characters")
    @Schema(description = "Primary guest name", example = "Mohana Prasath R")
    private String guestName;

    @NotNull(message = "Check-in date is required")
    @Schema(description = "Check-in arrival date", example = "2026-09-01")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Schema(description = "Check-out departure date", example = "2026-09-05")
    private LocalDate checkOutDate;

    @NotNull(message = "Guests count is required")
    @Min(value = 1, message = "Guests count must be at least 1")
    @Max(value = 10, message = "Guests count cannot exceed 10")
    @Schema(description = "Number of occupying guests", example = "2")
    private Integer guests;

    @NotNull(message = "Room type is required")
    @Schema(description = "Allocated room tier", example = "DELUXE")
    private RoomType roomType;

    @NotNull(message = "Booking status is required")
    @Schema(description = "Current lifecycle status", example = "CONFIRMED")
    private BookingStatus bookingStatus;

    @NotNull(message = "Total revenue is required")
    @DecimalMin(value = "0.00", message = "Total revenue cannot be negative")
    @Schema(description = "Total reservation transaction earnings in USD", example = "1250.00")
    private BigDecimal totalRevenue;
}
