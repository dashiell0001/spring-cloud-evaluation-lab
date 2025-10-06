package com.example.flightsearch.web;

import com.example.flightsearch.domain.Flight;
import com.example.flightsearch.service.FlightService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

  private final FlightService service;

  public FlightController(FlightService service) { this.service = service; }

  @GetMapping
  public ResponseEntity<List<Flight>> listAll() {
    return ResponseEntity.ok(service.listAll());
  }

  @GetMapping("/search")
  public ResponseEntity<List<Flight>> search(
      @RequestParam String origin,
      @RequestParam String destination,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
  ) {
    return ResponseEntity.ok(service.search(origin, destination, dateFrom, dateTo));
  }
}
