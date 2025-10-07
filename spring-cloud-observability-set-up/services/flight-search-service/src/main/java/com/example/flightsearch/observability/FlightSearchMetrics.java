package com.example.flightsearch.observability;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class FlightSearchMetrics implements InitializingBean {
    private final MeterRegistry meterRegistry;
    private AtomicInteger counter;

    @Override
    public void afterPropertiesSet() {
        counter = meterRegistry.gauge("flight_search_total", new AtomicInteger(0));
    }

    public void incrementSearches() {
        counter.incrementAndGet();
    }
}
