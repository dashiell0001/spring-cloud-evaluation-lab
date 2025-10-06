# API Gateway Setup and Configuration Guide

This guide explains how to set up, run, and reconfigure a **Spring Cloud API Gateway** integrated with Eureka and Config Server.  
It also shows how to expose routes for **Flight Search**, **Pricing**, and **Booking** services.

---

## 1. Prerequisites

- **Java 17+** installed
- **Maven/Gradle** to build the gateway project
- **Docker & Docker Compose** for databases and registry
- **Eureka Server** running on port `8761`
- **Spring Cloud Config Server** running on port `8090` with access to your config repo

---

## 2. Create and Run the API Gateway

1. Create a new Spring Boot project (via **Spring Initializr** or IntelliJ) with dependencies:
   - `spring-cloud-starter-gateway`
   - `spring-cloud-starter-netflix-eureka-client`
   - `spring-boot-starter-actuator`
   - `spring-cloud-starter-config`

2. In `application.yml` (local fallback config):
   ```yaml
   server:
     port: 8088

   spring:
     application:
       name: api-gateway
     cloud:
       config:
         uri: http://localhost:8090
         fail-fast: true

   eureka:
     client:
       service-url:
         defaultZone: http://localhost:8761/eureka/
   ```

3. Run the gateway:
   ```
   mvn spring-boot:run
   ```
   or
   ```
   java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
   ```

---

## 3. Configure Routes via Config Server

1. In your **Config Server repository**, add the file `api-gateway.yml` under the configs folder.

2. Example advanced configuration:

   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           - id: flights
             uri: lb://flight-search-service
             predicates:
               - Path=/api/flights/**
             filters:
               - SetRequestHeader=X-Correlation-Id, "#{T(java.util.UUID).randomUUID().toString()}"
               - name: Retry
                 args:
                   retries: 1
                   methods: GET
                   statuses: BAD_GATEWAY, GATEWAY_TIMEOUT, INTERNAL_SERVER_ERROR
               - name: CircuitBreaker
                 args:
                   name: flightsEdgeCB
                   fallbackUri: forward:/fallback/flights
             metadata:
               connect-timeout: 800
               response-timeout: 2500

           - id: bookings
             uri: lb://booking-service
             predicates:
               - Path=/api/bookings/**
             filters:
               - SetRequestHeader=X-Correlation-Id, "#{T(java.util.UUID).randomUUID().toString()}"
               - name: CircuitBreaker
                 args:
                   name: bookingsEdgeCB
                   fallbackUri: forward:/fallback/bookings
             metadata:
               connect-timeout: 1000
               response-timeout: 5000

           - id: pricing
             uri: lb://pricing-service
             predicates:
               - Path=/api/pricing/**
             filters:
               - SetRequestHeader=X-Correlation-Id, "#{T(java.util.UUID).randomUUID().toString()}"
               - name: CircuitBreaker
                 args:
                   name: pricingEdgeCB
                   fallbackUri: forward:/fallback/pricing
             metadata:
               connect-timeout: 600
               response-timeout: 2000
   ```

3. Commit the file and restart or refresh the gateway.

---

## 4. Refresh Configurations Without Restart

1. Call the refresh endpoint to reload Config Server values:
   ```bash
   curl -X POST http://localhost:8088/actuator/refresh
   curl -X POST http://localhost:8088/actuator/gateway/refresh
   ```

2. Verify updated routes:
   ```bash
   curl http://localhost:8088/actuator/gateway/routes
   ```

---

## 5. Test the Gateway

- **Flights**:
  ```bash
  curl "http://localhost:8088/api/flights/search?origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28"
  ```

- **Pricing**:
  ```bash
  curl "http://localhost:8088/api/pricing/quote?baseFare=100&currency=USD&bags=0&personType=ADULT&seatCount=2"
  ```

- **Bookings**:
  ```bash
  curl -X POST "http://localhost:8088/api/bookings" -H "Content-Type: application/json" -d '{"flightId":1,"seats":2,"passengerType":"ADULT"}'
  ```

- If one service is down, the fallback endpoint defined in the gateway should respond with a `503` JSON.

---

## 6. Add Fallback Controller

In the gateway app, add a simple fallback controller:

```java
@RestController
class GatewayFallbackController {
  @GetMapping("/fallback/flights")
  public ResponseEntity<?> flightsFallback() {
    return ResponseEntity.status(503).body(Map.of("message", "Flights unavailable", "status", 503));
  }

  @GetMapping("/fallback/bookings")
  public ResponseEntity<?> bookingsFallback() {
    return ResponseEntity.status(503).body(Map.of("message", "Bookings unavailable", "status", 503));
  }

  @GetMapping("/fallback/pricing")
  public ResponseEntity<?> pricingFallback() {
    return ResponseEntity.status(503).body(Map.of("message", "Pricing unavailable", "status", 503));
  }
}
```

---

## 7. Summary

- Gateway sits at the **edge** and routes traffic to microservices.  
- Configurations are **externalized** via Spring Cloud Config.  
- Routes support **timeouts, retries, circuit breakers, and header propagation**.  
- Use **actuator refresh** to reapply configuration changes without downtime.

