package com.example.flightsearch.service;

import java.math.BigDecimal;

public record PriceResponse(
        BigDecimal baseFare,
        BigDecimal seatFeeTotal,
        BigDecimal total,
        String currency
) {}
