# Flight Search Service (PostgreSQL edition)

Spring Boot 3 + Java 17 microservice exposing:
- `GET /api/flights` → list all
- `GET /api/flights/search?origin&destination&dateFrom&dateTo` → search by route & date range

Backed by **PostgreSQL** (Docker), with **unit tests**, **web layer tests**, and a **Testcontainers** integration test.

## Run locally (Postgres via Docker Compose)
```bash
mvn -DskipTests clean package
docker compose up --build
# app -> http://localhost:8080
# db  -> localhost:5432 (flight/flight)
```

## Test
```bash
mvn test
```

## Defaults with data
Try:
```
GET /api/flights/search?origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28
```
