package com.mohana.hotelanalytics.config;

import com.mohana.hotelanalytics.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Spring Boot Actuator HealthIndicator.
 * Probes database responsiveness, query latency, and record volume.
 */
@Component
@RequiredArgsConstructor
public class HotelAnalyticsHealthIndicator implements HealthIndicator {

    private final BookingRepository bookingRepository;

    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();
        try {
            long count = bookingRepository.count();
            long latencyMs = System.currentTimeMillis() - startTime;

            return Health.up()
                    .withDetail("database", "Available")
                    .withDetail("totalBookingsCount", count)
                    .withDetail("latencyMs", latencyMs)
                    .withDetail("status", "HEALTHY")
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("database", "Unreachable")
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
