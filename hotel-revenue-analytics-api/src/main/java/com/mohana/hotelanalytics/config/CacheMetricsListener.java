package com.mohana.hotelanalytics.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Monitors Spring Cache lifecycle events and records eviction metrics for analytics queries.
 */
@Slf4j
@Component
public class CacheMetricsListener {

    private final Counter cacheEvictionCounter;

    public CacheMetricsListener(MeterRegistry meterRegistry) {
        this.cacheEvictionCounter = Counter.builder("hotel.cache.evictions")
                .description("Total number of cache eviction operations triggered by booking mutations")
                .register(meterRegistry);
    }

    public void recordEviction() {
        cacheEvictionCounter.increment();
        log.debug("[CACHE_EVICTION] In-memory revenue analytics cache cleared");
    }
}
