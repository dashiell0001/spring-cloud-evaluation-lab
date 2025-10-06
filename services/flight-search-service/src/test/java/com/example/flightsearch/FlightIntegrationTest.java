package com.example.flightsearch;

import com.example.flightsearch.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@SuppressWarnings("all")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // no reemplazar por H2
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",                    // FORZAR ejecución de schema.sql / data.sql
        "spring.jpa.hibernate.ddl-auto=none",             // usamos nuestros scripts
        "spring.jpa.defer-datasource-initialization=true" // esperar a que el datasource esté listo antes de JPA
})
public class FlightIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("flights")
            .withUsername("flight")
            .withPassword("flight");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    FlightRepository repository;

    @Test
    void contextLoads_and_schemaInitialized() {
        assertThat(repository.findAll()).isNotEmpty();
    }
}

