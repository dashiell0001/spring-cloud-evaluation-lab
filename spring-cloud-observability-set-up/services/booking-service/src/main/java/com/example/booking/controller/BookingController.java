package com.example.booking.controller;

import com.example.booking.model.BookingRequest;
import com.example.booking.model.BookingResponse;
import com.example.booking.repository.BookingRepository;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

  private final BookingService service;
  private final BookingRepository repo;

  public BookingController(BookingService service, BookingRepository repo) {
    this.service = service;
    this.repo = repo;
  }

  @PostMapping
  public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
    return ResponseEntity.ok(service.create(request));
  }

  @GetMapping("/{recordLocator}")
  public ResponseEntity<?> byLocator(@PathVariable String recordLocator) {
    return repo.findByRecordLocator(recordLocator)
      .<ResponseEntity<?>>map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }
}