package com.example.flightsearch.service;

import com.example.flightsearch.dto.PricingQuote;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient("pricing-service")
public interface PricingClient {


    @GetMapping("/api/pricing/quote")
    @TimeLimiter(name = "pricing")
    PricingQuote quote(
            @RequestParam("baseFare") BigDecimal baseFare,
            @RequestParam("currency") String currency,
            @RequestParam("bags") int bags
    );


}