package com.example.booking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class Booking {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, unique = true, length = 8)
  private String recordLocator;
  @Column(nullable = false, length = 10)
  private String flightNumber;
  @Column(nullable = false, length = 3)
  private String origin;
  @Column(nullable = false, length = 3)
  private String destination;
  @Column(nullable = false)
  private LocalDate departureDate;
  @Column(nullable = false)
  private Integer seatCount;
  @Column(nullable = false, length = 10)
  private String personType; // ADULT/CHILD
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal totalFare;
  @Column(nullable = false, length = 3)
  private String currency;

  public Long getId() { return id; }
  public String getRecordLocator() { return recordLocator; }
  public void setRecordLocator(String recordLocator) { this.recordLocator = recordLocator; }
  public String getFlightNumber() { return flightNumber; }
  public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
  public String getOrigin() { return origin; }
  public void setOrigin(String origin) { this.origin = origin; }
  public String getDestination() { return destination; }
  public void setDestination(String destination) { this.destination = destination; }
  public LocalDate getDepartureDate() { return departureDate; }
  public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
  public Integer getSeatCount() { return seatCount; }
  public void setSeatCount(Integer seatCount) { this.seatCount = seatCount; }
  public String getPersonType() { return personType; }
  public void setPersonType(String personType) { this.personType = personType; }
  public BigDecimal getTotalFare() { return totalFare; }
  public void setTotalFare(BigDecimal totalFare) { this.totalFare = totalFare; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
}