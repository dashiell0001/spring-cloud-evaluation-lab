package com.example.flightsearch.repository;

import com.example.flightsearch.domain.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

  List<Flight> findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureDateBetween(
      String origin, String destination, LocalDate dateFrom, LocalDate dateTo);
}
