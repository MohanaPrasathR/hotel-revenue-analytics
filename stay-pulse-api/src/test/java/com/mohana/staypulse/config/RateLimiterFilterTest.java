package com.mohana.staypulse.config;

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

    @Test
    @DisplayName("Should evict expired client trackers and reset the window after it elapses")
    void shouldEvictStaleTrackersAndRollWindow() throws ServletException, IOException {
        java.util.concurrent.atomic.AtomicLong now = new java.util.concurrent.atomic.AtomicLong(1_000_000L);
        RateLimiterFilter filter = new RateLimiterFilter(1, now::get);

        for (String ip : new String[]{"10.0.0.1", "10.0.0.2", "10.0.0.3"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bookings");
            request.setRemoteAddr(ip);
            filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }
        assertThat(filter.trackedClientCount()).isEqualTo(3);

        now.addAndGet(61_000L);
        MockHttpServletRequest later = new MockHttpServletRequest("GET", "/api/bookings");
        later.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse laterResponse = new MockHttpServletResponse();
        filter.doFilter(later, laterResponse, filterChain);

        assertThat(laterResponse.getStatus()).isEqualTo(200);
        assertThat(filter.trackedClientCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should key clients by the first X-Forwarded-For address")
    void shouldUseFirstForwardedForAddress() throws ServletException, IOException {
        RateLimiterFilter strict = new RateLimiterFilter(1);
        for (String forwarded : new String[]{"203.0.113.1, 10.0.0.1", "203.0.113.2, 10.0.0.1"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bookings");
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", forwarded);
            MockHttpServletResponse response = new MockHttpServletResponse();
            strict.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Should fall back to X-Real-IP when X-Forwarded-For is absent")
    void shouldFallBackToRealIpHeader() throws ServletException, IOException {
        RateLimiterFilter strict = new RateLimiterFilter(1);
        for (String realIp : new String[]{"198.51.100.1", "198.51.100.2"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bookings");
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Real-IP", realIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            strict.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
