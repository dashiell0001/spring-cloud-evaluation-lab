package com.example.flightsearch.service;

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

    public List<PricedFlight> searchAndPrice(String origin, String destination,
                                             LocalDate dateFrom, LocalDate dateTo,
                                             String passengerType, int seats) {

        var flights = flightRepository.search(origin, destination, dateFrom, dateTo);

        return flights.stream()
                .map(f -> flightPricingService.priceFlight(f, passengerType, seats))
                .toList();
    }
}
