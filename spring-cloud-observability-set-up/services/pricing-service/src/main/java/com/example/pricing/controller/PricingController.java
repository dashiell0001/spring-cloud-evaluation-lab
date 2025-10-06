package com.example.pricing.controller;

import com.example.pricing.model.PricingQuote;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Validated
@RestController
public class PricingController {

  private static final BigDecimal TAX_RATE = new BigDecimal("0.21"); // 21%
  private static final BigDecimal BAG_FEE_UNIT = new BigDecimal("30.00"); // per bag
  private static final String RULES_VERSION = "v1";

  @GetMapping("/api/pricing/quote")
  public ResponseEntity<PricingQuote> quote(
      @RequestParam @NotNull BigDecimal baseFare,
      @RequestParam @NotBlank String currency,
      @RequestParam @Min(0) int bags
  ) {
    BigDecimal base = baseFare.setScale(2, RoundingMode.HALF_UP);
    String curr = currency.trim().toUpperCase();

    BigDecimal tax = base.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    BigDecimal bagFees = BAG_FEE_UNIT.multiply(BigDecimal.valueOf(bags)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal total = base.add(tax).add(bagFees).setScale(2, RoundingMode.HALF_UP);

    PricingQuote quote = new PricingQuote(base, tax, bagFees, total, curr, RULES_VERSION);
    return ResponseEntity.ok(quote);
  }
}
