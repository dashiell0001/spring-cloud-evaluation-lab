package com.example.flightsearch.service;

import com.example.flightsearch.domain.Flight;
import com.example.flightsearch.repository.FlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class FlightService {

  private final FlightRepository repository;

  public FlightService(FlightRepository repository) {
    this.repository = repository;
  }

  public List<Flight> listAll() { return repository.findAll(); }

    public List<Flight> search(String origin, String destination, LocalDate dateFrom, LocalDate dateTo) {
        String o = origin == null ? null : origin.trim();
        String d = destination == null ? null : destination.trim();

        if (o == null || StringUtils.hasText(o) || d == null || StringUtils.hasText(d)) {
            throw new IllegalArgumentException("origin and destination are required");
        }
        if (dateFrom == null || dateTo == null || dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must be <= dateTo");
        }

        return repository.findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureDateBetween(
                o, d, dateFrom, dateTo
        );
    }
}
