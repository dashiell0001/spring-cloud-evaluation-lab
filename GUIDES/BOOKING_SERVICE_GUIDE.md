# Booking Service — Setup & Usage Guide

This guide explains how to:
- Run **two Postgres databases** (flights & bookings) with Docker Compose.
- Build and run the **booking-service** (Spring Boot, JPA, Postgres).
- Understand the **end-to-end flow** across `flight-search-service`, `pricing-service`, and `booking-service`.
- Test the exposed endpoints.

---

## 1) Start Databases (Docker Compose)

Use the provided compose that brings up **both** DBs:
- `flights-db` → `jdbc:postgresql://localhost:5432/flights` (user: `flight` / pass: `flight`)
- `bookings-db` → `jdbc:postgresql://localhost:5433/bookings` (user: `booking` / pass: `booking`)

```bash
docker compose -f docker-compose-dbs.yml up -d
docker compose -f docker-compose-dbs.yml ps
```

> Ensure both containers are **healthy** before starting services.

---

## 2) Build & Run booking-service (Local JVM)

Download and unzip `booking-service.zip` (or open it in IntelliJ). Then:

```bash
cd booking-service
./mvnw spring-boot:run   # or: mvn spring-boot:run
```

Defaults in `application.yml`:
```yaml
server.port: 8082
spring.datasource.url: jdbc:postgresql://localhost:5433/bookings
spring.datasource.username: booking
spring.datasource.password: booking
spring.jpa.hibernate.ddl-auto: update
```

Health check:
```bash
curl http://localhost:8082/actuator/health
# {"status":"UP"}
```

---

## 3) Functionality & API

### 3.1 Create a booking
```
POST /api/bookings
Content-Type: application/json
```

**Request body:**
```json
{
  "flightNumber": "MX123",
  "origin": "MEX",
  "destination": "LAX",
  "departureDate": "2025-12-22",
  "seatCount": 2,
  "personType": "ADULT",
  "totalFare": 181.00,
  "currency": "USD"
}
```

**Response (200):**
```json
{
  "recordLocator": "AB12CD",
  "flightNumber": "MX123",
  "origin": "MEX",
  "destination": "LAX",
  "departureDate": "2025-12-22",
  "seatCount": 2,
  "personType": "ADULT",
  "totalFare": 181.00,
  "currency": "USD"
}
```

### 3.2 Get a booking by record locator
```
GET /api/bookings/{recordLocator}
```

**Example:**
```bash
curl http://localhost:8082/api/bookings/AB12CD
```

**404** if not found.

---

## 4) End-to-End Flow (3 microservices)

1. **flight-search-service**  
   - The client queries:
     ```
     GET /api/flights/search?origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28
     ```
   - Service retrieves flights from **flights-db**.
   - For each flight, it calls **pricing-service** (via OpenFeign/Eureka) to compute `tax`, `seatFees`, `totalFare` (for this lab: `bags=0`, pass `personType` & `seatCount` from the UI).
   - Returns flights enriched with pricing.

2. **booking-service**  
   - The client (or flight-search) POSTs to `booking-service` with the selected flight + seat count + person type + computed total fare.
   - `booking-service` stores booking into **bookings-db** and returns a `recordLocator` + echoed data.

3. **pricing-service**  
   - Pure computation service: calculates fares based on base fare, person type, seat count (and optionally bags).

---

## 5) Testing Quickly

### Create a booking
```bash
curl -X POST http://localhost:8082/api/bookings   -H "Content-Type: application/json"   -d '{
    "flightNumber": "MX123",
    "origin": "MEX",
    "destination": "LAX",
    "departureDate": "2025-12-22",
    "seatCount": 2,
    "personType": "ADULT",
    "totalFare": 181.00,
    "currency": "USD"
  }'
```

### Retrieve a booking
```bash
curl http://localhost:8082/api/bookings/XXXXXX
```

Replace `XXXXXX` with the `recordLocator` from the create response.

---

## 6) Docker Image (optional)

```bash
# Build jar
./mvnw -DskipTests clean package

# Build image
docker build -t booking-service:1.0.0 .

# Run
docker run --rm -p 8082:8082   -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5433/bookings   -e SPRING_DATASOURCE_USERNAME=booking   -e SPRING_DATASOURCE_PASSWORD=booking   booking-service:1.0.0
```

> On Linux, replace `host.docker.internal` with your host IP.

---

## 7) Next Steps (Resilience Labs)

- **Retry + TimeLimiter**: Around `flight-search → pricing` calls (idempotent GET).  
- **Circuit Breaker**: Protect `flight-search` from cascading failure when `pricing` is unstable.  
- **Bulkhead**: Cap concurrent calls from `flight-search` to `pricing`.

You now have a functional **booking-service** with persistence and a two-DB setup ready for resilience experiments.
