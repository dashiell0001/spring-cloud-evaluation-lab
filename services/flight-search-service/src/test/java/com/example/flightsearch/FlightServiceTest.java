package com.example.flightsearch;

import com.example.flightsearch.domain.Flight;
import com.example.flightsearch.repository.FlightRepository;
import com.example.flightsearch.service.FlightService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FlightServiceTest {

    @Test
    void search_validates_and_queries_repository() {
        FlightRepository repo = Mockito.mock(FlightRepository.class);
        FlightService service = new FlightService(repo);

        String origin = "MEX";
        String destination = "LAX";
        LocalDate from = LocalDate.parse("2025-12-20");
        LocalDate to = LocalDate.parse("2025-12-28");

        when(repo.findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureDateBetween(origin, destination, from, to))
                .thenReturn(List.of(new Flight()));

        List<Flight> result = service.search(origin, destination, from, to);
        assertEquals(1, result.size());

        verify(repo).findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureDateBetween(origin, destination, from, to);
    }

    @Test
    void search_throws_on_invalid_dates() {
        FlightRepository repo = Mockito.mock(FlightRepository.class);
        FlightService service = new FlightService(repo);

        LocalDate from = LocalDate.parse("2025-12-29");
        LocalDate to = LocalDate.parse("2025-12-28");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.search("MEX", "LAX", from, to));

        assertTrue(ex.getMessage() != null && ex.getMessage().toLowerCase().contains("datefrom"));
        verifyNoInteractions(repo);
    }
}
