package com.example.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
class GatewayFallbackController {
    @GetMapping("/fallback/flights")
    public ResponseEntity<?> flightsFallback() {
        return ResponseEntity.status(503).body(Map.of("message", "Flights unavailable", "status", 503));
    }

    @GetMapping("/fallback/bookings")
    public ResponseEntity<?> bookingsFallback() {
        return ResponseEntity.status(503).body(Map.of("message", "Bookings unavailable", "status", 503));
    }

    @GetMapping("/fallback/pricing")
    public ResponseEntity<?> pricingFallback() {
        return ResponseEntity.status(503).body(Map.of("message", "Pricing unavailable", "status", 503));
    }
}