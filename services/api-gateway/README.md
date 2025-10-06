# API Gateway — Ready-to-Run (Eureka + Externalized Config)

This project is a minimal Spring Cloud Gateway that relies on **Spring Cloud Config Server** for:
- Eureka settings (service discovery)
- Gateway routes (expose flight-search, pricing, booking)

Local config contains only **port**, **application name**, and **Config Server import**.

## Prereqs
- Java 17+, Maven 3.9+
- Config Server running at `http://localhost:8090`
- Eureka Server running at `http://localhost:8761`
- Services: flight-search-service, pricing-service, booking-service

## Config Server entry (example `api-gateway.yml`)
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lowerCaseServiceId: true
      routes:
        - id: flights
          uri: lb://flight-search-service
          predicates: [ Path=/api/flights/** ]
        - id: pricing
          uri: lb://pricing-service
          predicates: [ Path=/api/pricing/** ]
        - id: bookings
          uri: lb://booking-service
          predicates: [ Path=/api/bookings/** ]
management:
  endpoints.web.exposure.include: health,info,metrics
```

## Run
```bash
mvn spring-boot:run
# or:
mvn -DskipTests clean package
java -jar target/api-gateway-1.0.0.jar
```

## Smoke tests (via gateway)
```bash
curl "http://localhost:8088/api/flights/search?origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28"
curl "http://localhost:8088/api/pricing/quote?baseFare=100&currency=USD&bags=0&personType=ADULT&seatCount=2"
curl -X POST "http://localhost:8088/api/bookings" -H "Content-Type: application/json" -d '{"flightNumber":"MX123","origin":"MEX","destination":"LAX","departureDate":"2025-12-22","seatCount":2,"personType":"ADULT","totalFare":181.00,"currency":"USD"}'
```
