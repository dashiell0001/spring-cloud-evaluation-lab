package com.example.booking.model;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingResponse(
        String recordLocator,
        String flightNumber,
        String origin,
        String destination,
        LocalDate departureDate,
        int seatCount,
        String personType,
        BigDecimal totalFare,
        String currency
) {
}