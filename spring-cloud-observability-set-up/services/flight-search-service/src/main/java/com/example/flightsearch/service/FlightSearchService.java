package com.example.flightsearch.service;

import com.example.flightsearch.observability.FlightSearchMetrics;
import com.example.flightsearch.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightSearchService {

    private final FlightRepository flightRepository;
    private final FlightPricingService flightPricingService;
    private final FlightSearchMetrics flightSearchMetrics;

    public List<PricedFlight> searchAndPrice(String origin, String destination,
                                             LocalDate dateFrom, LocalDate dateTo,
                                             String passengerType, int seats) {

        var flights = flightRepository.findByOriginAndDestinationAndDepartureDateBetween(origin, destination, dateFrom, dateTo);

        flightSearchMetrics.incrementSearches();

        return flights.stream()
                .map(f -> flightPricingService.priceFlight(f, passengerType, seats))
                .toList();
    }
}
