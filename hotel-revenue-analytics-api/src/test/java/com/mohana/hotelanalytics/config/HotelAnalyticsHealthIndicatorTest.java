package com.mohana.hotelanalytics.config;

import com.mohana.hotelanalytics.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotelAnalyticsHealthIndicatorTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private HotelAnalyticsHealthIndicator healthIndicator;

    @Test
    @DisplayName("Should return UP health status with booking metrics when database is accessible")
    void health_DatabaseUp_ReturnsUp() {
        when(bookingRepository.count()).thenReturn(25L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("database", "Available");
        assertThat(health.getDetails()).containsEntry("totalBookingsCount", 25L);
        assertThat(health.getDetails()).containsKey("latencyMs");
    }

    @Test
    @DisplayName("Should return DOWN health status when database query fails")
    void health_DatabaseDown_ReturnsDown() {
        when(bookingRepository.count()).thenThrow(new RuntimeException("Connection timed out"));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("database", "Unreachable");
        assertThat(health.getDetails()).containsEntry("error", "Connection timed out");
    }
}
