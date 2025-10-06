package com.example.flightsearch.service;


import com.example.flightsearch.domain.Flight;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PricedFlight(
        Flight flight,
        BigDecimal totalPrice,
        String currency
) {}