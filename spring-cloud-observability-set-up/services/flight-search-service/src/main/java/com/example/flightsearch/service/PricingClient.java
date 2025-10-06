package com.example.flightsearch.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "pricing-service", path = "/api/pricing")
public interface PricingClient {

    @GetMapping("/quote")
    PriceResponse quote(
            @RequestParam String flightNumber,
            @RequestParam String date,
            @RequestParam String passengerType,
            @RequestParam int seats
    );
}
