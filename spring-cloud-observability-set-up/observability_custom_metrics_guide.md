# Observability: JSON Logging with Logback + Loki/Promtail & Custom Metrics

This guide completes the observability setup by **adding Logback JSON-to-file logging** per service and wiring **Promtail → Loki → Grafana**. It also keeps **Prometheus** for metrics and shows how to expose **custom Micrometer counters**.

> Scope: local dev (apps run from IntelliJ/CLI), only observability + DBs in Docker.

---

## 1) Prerequisites
- Docker & Docker Compose
- Java 17, Maven
- Spring Boot 3.x services: `flight-search-service`, `pricing-service`, `booking-service`, `api-gateway`
- Your project layout similar to:
```
observability-set-up/
├─ docker-observability/
│  ├─ docker-compose.observability.yml
│  ├─ prometheus.yml
│  ├─ promtail-config.yml
├─ services/
│  ├─ flight-search-service/
│  │  ├─ src/main/resources/{application.yml, logback-spring.xml}
│  │  └─ logs/                      # JSON log files will be written here
│  ├─ pricing-service/...
│  ├─ booking-service/...
│  └─ api-gateway/...
```

Create the `logs/` folder in each service root (same level as `src/`).

---

## 2) Add Dependencies (every service)
```xml
<!-- Observability / metrics -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Tracing (optional but recommended to enrich logs with traceId/spanId via MDC) -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

<!-- JSON logging -->
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>7.4</version>
</dependency>
```

> Keep logging levels in Config Server (`logging.level.*`). Do **not** set `logging.pattern.*` or `logging.file.*` in YAML; Logback will handle format & file output.

---

## 3) Logback JSON + Rolling File (every service)

Create/Modify `src/main/resources/logback-spring.xml`:
```xml
<configuration scan="true" scanPeriod="30 seconds">
  <!-- Defaults without env/profile -->
  <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="application"/>
  <property name="ENV" value="local"/>
  <property name="LOG_DIR" value="./logs"/>
  <property name="LOG_MAX_HISTORY" value="7"/>
  <property name="LOG_MAX_SIZE" value="20MB"/>

  <!-- Console JSON (useful while running locally or inside containers) -->
  <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
      <providers>
        <timestamp/>
        <logLevel/>
        <threadName/>
        <loggerName/>
        <message fieldName="log_message"/>
        <mdc/>          <!-- includes traceId/spanId if Micrometer Tracing is on classpath -->
        <context/>
        <stackTrace/>
        <globalCustomFields>{"service_name":"${APP_NAME}","environment":"${ENV}"}</globalCustomFields>
      </providers>
    </encoder>
  </appender>

  <!-- File JSON (rolling by size+time) -->
  <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_DIR}/${APP_NAME}.log</file>
    <append>true</append>
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
      <providers>
        <timestamp/><logLevel/><threadName/><loggerName/>
        <message fieldName="log_message"/><mdc/><context/><stackTrace/>
        <globalCustomFields>{"service_name":"${APP_NAME}","environment":"${ENV}"}</globalCustomFields>
      </providers>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <fileNamePattern>${LOG_DIR}/${APP_NAME}-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
      <maxFileSize>${LOG_MAX_SIZE}</maxFileSize>
      <maxHistory>${LOG_MAX_HISTORY}</maxHistory>
    </rollingPolicy>
  </appender>

  <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="JSON_CONSOLE"/>
    <appender-ref ref="JSON_FILE"/>
    <queueSize>8192</queueSize>
    <discardingThreshold>0</discardingThreshold>
  </appender>

  <root level="INFO">
    <appender-ref ref="ASYNC"/>
  </root>
</configuration>
```

> This writes JSON logs to `./logs/<service>.log` and rotates daily and by size. Promtail will tail these files.

---

## 4) Minimal Spring configuration (per service)

`src/main/resources/application.yml` (only observability bits; datasource, eureka, etc. go in Config Server as you already set up):
```yaml
spring:
  application:
    name: flight-search-service   # change per service
  config:
    import: "configserver:http://localhost:8090"

management:
  endpoints.web.exposure.include: health,info,metrics,prometheus,loggers
  endpoint.health.probes.enabled: true
  tracing.sampling.probability: 1.0

otel.exporter.otlp.endpoint: http://localhost:4317   # Jaeger OTLP if used
```

Create the folder for logs (once per service):
```
services/flight-search-service/logs
services/booking-service/logs
services/pricing-service/logs
services/api-gateway/logs
```

---

## 5) Promtail configuration (centralized file scraping)

`docker-observability/promtail-config.yml`:
```yaml
server:
  http_listen_port: 9081

clients:
  - url: http://host.docker.internal:3100/loki/api/v1/push

positions:
  filename: /tmp/positions.yaml

scrape_configs:
  - job_name: spring-microservices
    static_configs:
      - targets: [localhost]
        labels:
          job: spring-microservices
          __path__: /var/log/services/*/logs/*.log
```

This configuration tails all services’ logs mounted under `/var/log/services/<service>/logs/*.log`.

---

## 6) Prometheus config (metrics scraping)

`docker-observability/prometheus.yml`:
```yaml
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: 'spring-apps'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - 'host.docker.internal:8080'  # flight-search
          - 'host.docker.internal:8081'  # pricing
          - 'host.docker.internal:8082'  # booking
          - 'host.docker.internal:8088'  # api-gateway
```

---

## 7) Docker Compose (observability + DBs)

`docker-observability/docker-compose.observability.yml`:
```yaml
version: "3.9"

services:
  flights-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: flights
      POSTGRES_USER: flight
      POSTGRES_PASSWORD: flight
    ports: ["5435:5432"]
    volumes: [flights_pgdata:/var/lib/postgresql/data]

  bookings-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: bookings
      POSTGRES_USER: booking
      POSTGRES_PASSWORD: booking
    ports: ["5436:5432"]
    volumes: [bookings_pgdata:/var/lib/postgresql/data]

  loki:
    image: grafana/loki:2.9.4
    command: -config.file=/etc/loki/local-config.yaml
    ports:
      - "3100:3100"

  promtail:
    image: grafana/promtail:2.9.4
    volumes:
      - ./promtail-config.yml:/etc/promtail/config.yml:ro
      - ../services:/var/log/services:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"
    command: -config.file=/etc/promtail/config.yml

  prometheus:
    image: prom/prometheus:latest
    command: --config.file=/etc/prometheus/prometheus.yml
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"
    ports:
      - "9091:9090"

  grafana:
    image: grafana/grafana:latest
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    ports:
      - "3000:3000"

volumes:
  flights_pgdata:
  bookings_pgdata:
```

> 🔑 **Mount** `../services` so Promtail can see all `./logs/*.log` files from each service.

---

## 8) Custom metrics (Micrometer Counters)

**Flight Search — count searches:**
```java
package com.example.flightsearch.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class FlightSearchMetrics {
  private final Counter searchCounter;
  public FlightSearchMetrics(MeterRegistry registry) {
    this.searchCounter = Counter.builder("flight_search_total")
        .description("Total number of flight searches performed")
        .tag("service","flight-search-service")
        .register(registry);
  }
  public void increment() { searchCounter.increment(); }
}
```

**Booking — count bookings:**
```java
package com.example.booking.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BookingMetrics {
  private final Counter createdCounter;
  public BookingMetrics(MeterRegistry registry) {
    this.createdCounter = Counter.builder("booking_created_total")
        .description("Number of bookings successfully created")
        .tag("service","booking-service")
        .register(registry);
  }
  public void increment() { createdCounter.increment(); }
}
```

Call `metrics.increment()` in your controller/service at the moment of a successful search/booking.

---

## 9) Run & Validate

1) **Start the stack**
```
cd docker-observability
docker compose -f docker-compose.observability.yml up -d
```

2) **Run services locally** (each in its own terminal or from IDE):
```
mvn -q -f services/flight-search-service clean spring-boot:run
mvn -q -f services/booking-service clean spring-boot:run
mvn -q -f services/pricing-service clean spring-boot:run
mvn -q -f services/api-gateway clean spring-boot:run[observability_visualization_steps.md](../Desktop/observability_visualization_steps.md)
```

3) **Generate traffic**
```
curl "http://localhost:8081/api/flights/search?origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28"
curl -X POST "http://localhost:8082/api/bookings" -H "Content-Type: application/json" -d '{...}'
```

4) **Check logs in Grafana → Explore → Loki**
- Data source: Loki
- Query (LogQL): `{job="spring-microservices"}` or `{filename=~".*flight-search-service.*"}`

5) **Check metrics**
- Prometheus: http://localhost:9091 → *Status → Targets* (should be UP)
- Grafana: http://localhost:3000 (admin/admin)
  - Add data source: Prometheus `http://host.docker.internal:9091`
  - Explore: `rate(flight_search_total[1m])`, `rate(booking_created_total[1m])`

---

## 11) Summary
- JSON structured logs are written per service and collected by **Promtail → Loki → Grafana**.
- Custom Micrometer counters expose business KPIs via `/actuator/prometheus`.
- All the pieces run locally: services from IDE, observability stack + DBs in Docker.
