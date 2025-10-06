package com.example.flightsearch.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "flights")
public class Flight {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 8)
  private String origin;

  @Column(nullable = false, length = 8)
  private String destination;

  @Column(name = "departure_date", nullable = false)
  private LocalDate departureDate;

  @Column(name = "return_date")
  private LocalDate returnDate;

  @Column(nullable = false, length = 2)
  private String airline;

  @Column(name = "flight_number", nullable = false, length = 10)
  private String flightNumber;

  @Column(nullable = false, length = 16)
  private String cabin;

  @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
  private java.math.BigDecimal baseFare;

  @Column(name = "total_fare", nullable = false, precision = 12, scale = 2)
  private java.math.BigDecimal totalFare;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "seats_available", nullable = false)
  private Integer seatsAvailable;

  public Flight() {}

  public Flight(String origin, String destination, LocalDate departureDate, LocalDate returnDate,
                String airline, String flightNumber, String cabin, BigDecimal baseFare,
                BigDecimal totalFare, String currency, Integer seatsAvailable) {
    this.origin = origin;
    this.destination = destination;
    this.departureDate = departureDate;
    this.returnDate = returnDate;
    this.airline = airline;
    this.flightNumber = flightNumber;
    this.cabin = cabin;
    this.baseFare = baseFare;
    this.totalFare = totalFare;
    this.currency = currency;
    this.seatsAvailable = seatsAvailable;
  }

  public Long getId() { return id; }
  public String getOrigin() { return origin; }
  public String getDestination() { return destination; }
  public LocalDate getDepartureDate() { return departureDate; }
  public LocalDate getReturnDate() { return returnDate; }
  public String getAirline() { return airline; }
  public String getFlightNumber() { return flightNumber; }
  public String getCabin() { return cabin; }
  public java.math.BigDecimal getBaseFare() { return baseFare; }
  public java.math.BigDecimal getTotalFare() { return totalFare; }
  public String getCurrency() { return currency; }
  public Integer getSeatsAvailable() { return seatsAvailable; }

  public void setId(Long id) { this.id = id; }
  public void setOrigin(String origin) { this.origin = origin; }
  public void setDestination(String destination) { this.destination = destination; }
  public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
  public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
  public void setAirline(String airline) { this.airline = airline; }
  public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
  public void setCabin(String cabin) { this.cabin = cabin; }
  public void setBaseFare(java.math.BigDecimal baseFare) { this.baseFare = baseFare; }
  public void setTotalFare(java.math.BigDecimal totalFare) { this.totalFare = totalFare; }
  public void setCurrency(String currency) { this.currency = currency; }
  public void setSeatsAvailable(Integer seatsAvailable) { this.seatsAvailable = seatsAvailable; }
}
