package com.example.flightsearch.web;

import com.example.flightsearch.service.FlightSearchService;
import com.example.flightsearch.service.PricedFlight;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightSearchService service;

    @GetMapping("/search")
    public List<PricedFlight> search(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "ADULT") String passengerType,
            @RequestParam(defaultValue = "1") int seats
    ) {
        return service.searchAndPrice(
                origin, destination,
                dateFrom, dateTo,
                passengerType, seats
        );
    }
}
