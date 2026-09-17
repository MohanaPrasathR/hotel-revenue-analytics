package com.mohana.hotelanalytics.config;

import com.mohana.hotelanalytics.repository.BookingRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Registers Micrometer gauges and operational metrics for Prometheus and Actuator.
 */
@Component
@RequiredArgsConstructor
public class AnalyticsMetricsContributor {

    private final MeterRegistry meterRegistry;
    private final BookingRepository bookingRepository;

    @PostConstruct
    public void initMetrics() {
        Gauge.builder("hotel.revenue.total", this, AnalyticsMetricsContributor::fetchTotalRevenue)
                .description("Total cumulative realized revenue across all hotels in INR")
                .register(meterRegistry);

        Gauge.builder("hotel.bookings.total.count", bookingRepository, BookingRepository::count)
                .description("Total number of bookings registered in database")
                .register(meterRegistry);
    }

    private double fetchTotalRevenue() {
        BigDecimal total = bookingRepository.findTotalRevenueOfActiveBookings();
        return total != null ? total.doubleValue() : 0.0;
    }
}
