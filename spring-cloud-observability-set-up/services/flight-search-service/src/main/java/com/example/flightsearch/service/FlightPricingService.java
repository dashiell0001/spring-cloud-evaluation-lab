package com.example.flightsearch.service;

import com.example.flightsearch.domain.Flight;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Service
@RequiredArgsConstructor
public class FlightPricingService {

    private final PricingClient pricingClient;

    @Retry(name = "pricing")
    @CircuitBreaker(name = "pricing", fallbackMethod = "priceFallback")
    public PricedFlight priceFlight(Flight flight, String passengerType, int seats) {
        requireNonNull(flight, "flight is required");
        var date = flight.getDepartureDate().toString();

        final var quote = pricingClient.quote(
                flight.getFlightNumber(),
                date,
                passengerType,
                seats
        );

        return PricedFlight.builder()
                .flight(flight)
                .totalPrice(quote.total())
                .currency(quote.currency())
                .build();
    }

    private PricedFlight priceFallback(Flight flight, String passengerType, int seats, Throwable ex) {
        var degradedPrice = flight.getBaseFare() != null ? flight.getBaseFare() : BigDecimal.ZERO;
        return PricedFlight.builder()
                .flight(flight)
                .totalPrice(degradedPrice)
                .currency("USD")
                .build();
    }
}