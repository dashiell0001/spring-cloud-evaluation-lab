# Observability Environment Setup and Custom Metrics Guide

## 1. Objective
This guide explains how to **set up a complete observability stack** using Docker Compose and **add custom metrics** in the Spring Boot services (`flight-search-service` and `booking-service`).

---

## 2. Prerequisites
Before proceeding, ensure you have:
- **Docker** and **Docker Compose** installed.
- **Java 17** and **Maven 3.8+**.
- Spring Boot 3.x microservices (`flight-search-service`, `booking-service`, etc.).
- Local PostgreSQL instances (can be part of the compose stack).

---

## 3. Docker Compose Observability Stack

### `docker-compose.yml`
```yaml
version: "3.9"

services:
  flights-db:
    image: postgres:16-alpine
    container_name: flights-postgres
    environment:
      POSTGRES_DB: flights
      POSTGRES_USER: flight
      POSTGRES_PASSWORD: flight
    ports:
      - "5432:5432"
    volumes:
      - flights_pgdata:/var/lib/postgresql/data

  bookings-db:
    image: postgres:16-alpine
    container_name: bookings-postgres
    environment:
      POSTGRES_DB: bookings
      POSTGRES_USER: booking
      POSTGRES_PASSWORD: booking
    ports:
      - "5433:5432"
    volumes:
      - bookings_pgdata:/var/lib/postgresql/data

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin

  loki:
    image: grafana/loki:latest
    container_name: loki
    ports:
      - "3100:3100"

  promtail:
    image: grafana/promtail:latest
    container_name: promtail
    volumes:
      - ./promtail-config.yml:/etc/promtail/config.yml
      - ../flight-search-service/logs:/var/log/flight-search
      - ../booking-service/logs:/var/log/bookings
    ports:
      - "9080:9080"

volumes:
  flights_pgdata:
  bookings_pgdata:
```

---

## 4. Prometheus Configuration

### `prometheus.yml`
```yaml
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: 'flight-search-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']

  - job_name: 'booking-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8081']
```

---

## 5. Spring Boot Configuration for Micrometer and Prometheus

Add these dependencies to each microservice:

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

In each `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    prometheus:
      enabled: true
```

---

## 6. Adding Custom Metrics

### Example 1: `FlightSearchMetrics.java`
```java
package com.example.flightsearch.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FlightSearchMetrics {

    private final AtomicInteger totalSearches;

    public FlightSearchMetrics(MeterRegistry registry) {
        this.totalSearches = registry.gauge("flight_search_total", new AtomicInteger(0));
    }

    public void incrementSearches() {
        totalSearches.incrementAndGet();
    }
}
```

### Example 2: `BookingMetrics.java`
```java
package com.example.booking.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BookingMetrics {

    private final AtomicInteger totalBookings;

    public BookingMetrics(MeterRegistry registry) {
        this.totalBookings = registry.gauge("booking_total", new AtomicInteger(0));
    }

    public void incrementBookings() {
        totalBookings.incrementAndGet();
    }
}
```

Use these metrics in your service logic to record activity.

---

## 7. Run the Environment

```
docker-compose up -d
```

Once up, you can verify:

| Service | URL | Default Credentials |
|----------|-----|---------------------|
| Prometheus | http://localhost:9090 | N/A |
| Grafana | http://localhost:3000 | admin / admin |
| Loki | http://localhost:3100 | N/A |

---

## 8. Visualizing Metrics in Grafana

1. Open Grafana → Add **Prometheus** data source at `http://prometheus:9090`.
2. Create a new dashboard and add panels for:
   - `flight_search_total`
   - `booking_total`

3. Run several searches or bookings, then refresh the dashboard to see live metrics.

---

## 9. Verify Logs and Traces

Logs from each microservice are sent to **Promtail → Loki → Grafana Explore**.

Go to **Grafana → Explore → Loki** and filter by labels such as `service_name="flight-search-service"`.

---

## 10. Summary

- The environment provides **logs (Loki)**, **metrics (Prometheus)**, and **visualization (Grafana)**.
- Each microservice exposes metrics at `/actuator/prometheus`.
- Custom metrics (`flight_search_total`, `booking_total`) allow monitoring domain activity.
