package com.mohana.hotelanalytics.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterFilterTest {

    private RateLimiterFilter rateLimiterFilter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimiterFilter = new RateLimiterFilter(3); // low limit for fast testing
    }

    @Test
    @DisplayName("Should permit requests within limit and attach rate limit headers")
    void shouldPermitRequestsWithinLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bookings");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimiterFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("3");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("2");
        assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should return HTTP 429 when client exceeds request limit")
    void shouldBlockWhenRateLimitExceeded() throws ServletException, IOException {
        String clientIp = "10.0.0.12";

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/bookings");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimiterFilter.doFilter(req, res, filterChain);
            assertThat(res.getStatus()).isEqualTo(200);
        }

        // 4th request should be throttled
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/bookings");
        req.setRemoteAddr(clientIp);
        MockHttpServletResponse res = new MockHttpServletResponse();
        rateLimiterFilter.doFilter(req, res, filterChain);

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isNotNull();
        assertThat(res.getContentAsString()).contains("Rate limit exceeded");
        verify(filterChain, times(3)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should bypass rate limiting for actuator and swagger endpoints")
    void shouldBypassActuatorAndSwaggerEndpoints() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimiterFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
