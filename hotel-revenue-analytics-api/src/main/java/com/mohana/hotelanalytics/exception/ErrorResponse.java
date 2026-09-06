package com.mohana.hotelanalytics.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error details returned on API exceptions")
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp when error occurred", example = "2026-09-06T11:25:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error reason phrase", example = "Bad Request")
    private String error;

    @Schema(description = "Application specific machine-readable error code", example = "INVALID_BOOKING_DATES")
    private String errorCode;

    @Schema(description = "Human-readable description of error", example = "Check-out date must be strictly after check-in date")
    private String message;

    @Schema(description = "Target API path of the request", example = "/api/bookings")
    private String path;

    @Schema(description = "Correlation trace ID for request debugging", example = "req-4a8f9c")
    private String traceId;

    @Schema(description = "Field-level validation error key-value mapping")
    private Map<String, String> validationErrors;
}

