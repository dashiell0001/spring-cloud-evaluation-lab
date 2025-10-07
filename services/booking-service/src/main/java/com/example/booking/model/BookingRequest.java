package com.example.booking.model;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingRequest(
        @NotBlank String flightNumber,
        @NotBlank @Size(min = 3, max = 3) String origin,
        @NotBlank @Size(min = 3, max = 3) String destination,
        @NotNull LocalDate departureDate,
        @Min(1) int seatCount,
        @Pattern(regexp = "ADULT|CHILD") String personType,
        @NotNull BigDecimal totalFare,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
