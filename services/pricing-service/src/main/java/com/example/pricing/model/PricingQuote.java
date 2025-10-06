package com.example.pricing.model;

import java.math.BigDecimal;

public record PricingQuote(
    BigDecimal baseFare,
    BigDecimal tax,
    BigDecimal bagFees,
    BigDecimal totalFare,
    String currency,
    String rulesVersion
) {}
