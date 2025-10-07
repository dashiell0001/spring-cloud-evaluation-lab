package com.example.booking.observation;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class BookingMetrics implements InitializingBean {

    private final MeterRegistry meterRegistry;
    private AtomicInteger counter;

    @Override
    public void afterPropertiesSet() {
        counter = meterRegistry.gauge("booking_total", new AtomicInteger(0));
    }

    public void incrementBooking() {
        counter.incrementAndGet();
    }
}
