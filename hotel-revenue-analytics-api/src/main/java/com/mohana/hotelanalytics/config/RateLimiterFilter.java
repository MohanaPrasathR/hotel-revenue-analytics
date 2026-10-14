package com.mohana.hotelanalytics.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding window rate limiting filter for REST API endpoints.
 * Enforces request throttling per client IP address.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RateLimiterFilter extends OncePerRequestFilter {

    private static final int DEFAULT_MAX_REQUESTS_PER_MINUTE = 100;
    private static final long WINDOW_SIZE_MS = 60_000L;

    private final int maxRequests;
    private final ConcurrentHashMap<String, ClientRequestTracker> clientTrackers = new ConcurrentHashMap<>();

    public RateLimiterFilter() {
        this(DEFAULT_MAX_REQUESTS_PER_MINUTE);
    }

    public RateLimiterFilter(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator") ||
               path.startsWith("/h2-console") ||
               path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        long currentTime = System.currentTimeMillis();

        ClientRequestTracker tracker = clientTrackers.computeIfAbsent(clientIp, ip -> new ClientRequestTracker(currentTime));
        int currentCount = tracker.registerHit(currentTime);
        long resetSeconds = Math.max(1, (WINDOW_SIZE_MS - (currentTime - tracker.windowStart())) / 1000);

        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequests - currentCount)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));

        if (currentCount > maxRequests) {
            log.warn("[RATE_LIMIT_EXCEEDED] Client IP {} exceeded rate limit of {} req/min (count: {})",
                    clientIp, maxRequests, currentCount);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(resetSeconds));

            String errorPayload = String.format(
                    "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in %d seconds.\",\"path\":\"%s\"}",
                    java.time.Instant.now(), resetSeconds, request.getRequestURI());

            response.getWriter().write(errorPayload);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    public void resetClientTracker(String clientIp) {
        clientTrackers.remove(clientIp);
    }

    private static class ClientRequestTracker {
        private long windowStartTime;
        private int requestCount;

        ClientRequestTracker(long startTime) {
            this.windowStartTime = startTime;
        }

        /** Registers a hit, rolling the window over atomically when it has expired. */
        synchronized int registerHit(long now) {
            if (now - windowStartTime > WINDOW_SIZE_MS) {
                windowStartTime = now;
                requestCount = 0;
            }
            return ++requestCount;
        }

        synchronized long windowStart() {
            return windowStartTime;
        }
    }
}
