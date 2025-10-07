package com.example.booking.service;

import com.example.booking.model.Booking;
import com.example.booking.model.BookingRequest;
import com.example.booking.model.BookingResponse;
import com.example.booking.observation.BookingMetrics;
import com.example.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class BookingService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final BookingRepository repo;
    private final SecureRandom rnd = new SecureRandom();
    private final BookingMetrics bookingMetrics;

    //    @Observed(name = "booking_total")
    @Transactional
    public BookingResponse create(BookingRequest req) {
        Booking b = new Booking();
        b.setRecordLocator(generateLocator(6));
        b.setFlightNumber(req.flightNumber());
        b.setOrigin(req.origin().toUpperCase());
        b.setDestination(req.destination().toUpperCase());
        b.setDepartureDate(req.departureDate());
        b.setSeatCount(req.seatCount());
        b.setPersonType(req.personType());
        b.setTotalFare(req.totalFare());
        b.setCurrency(req.currency().toUpperCase());
        repo.save(b);
        bookingMetrics.incrementBooking();
        return new BookingResponse(
                b.getRecordLocator(), b.getFlightNumber(), b.getOrigin(), b.getDestination(),
                b.getDepartureDate(), b.getSeatCount(), b.getPersonType(), b.getTotalFare(), b.getCurrency()
        );
    }

    private String generateLocator(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}