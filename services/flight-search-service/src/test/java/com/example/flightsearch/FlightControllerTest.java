package com.example.flightsearch;

import com.example.flightsearch.domain.Flight;
import com.example.flightsearch.service.FlightService;
import com.example.flightsearch.web.FlightController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ActiveProfiles("h2")
@WebMvcTest(controllers = FlightController.class)
public class FlightControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FlightService service;

    @Test
    void search_returns_200_and_array() throws Exception {
        when(service.search(anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(new Flight()));

        mvc.perform(get("/api/flights/search")
                        .param("origin", "MEX")
                        .param("destination", "LAX")
                        .param("dateFrom", "2025-12-20")
                        .param("dateTo", "2025-12-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
