# Flight Search Service (Spring Boot 3 + Java 17 + H2)

A minimal microservice for a travel platform that exposes a **flight search** endpoint backed by **H2** in-memory database.
This project is the starting point for integrating Spring Cloud (Config, Eureka, Gateway, Feign, Resilience, Observability, Stream, Security, Contract) in later labs.

---

## 📌 Endpoint to Implement (and Already Included)

**Search flights by route and date range**  
```
GET /api/flights/search?origin={IATA}&destination={IATA}&dateFrom=YYYY-MM-DD&dateTo=YYYY-MM-DD
```

### Parameters
- `origin` (required): IATA-like origin code, e.g., `MEX`, `GDL`, `GRU`.
- `destination` (required): IATA-like destination code, e.g., `LAX`, `JFK`, `MIA`.
- `dateFrom` (required, ISO): inclusive lower bound, e.g., `2025-12-20`.
- `dateTo` (required, ISO): inclusive upper bound, e.g., `2025-12-28`.

### Response
`200 OK` → JSON array of flight offers:
```json
[
  {
    "id": 1,
    "origin": "MEX",
    "destination": "LAX",
    "departureDate": "2025-12-20",
    "returnDate": null,
    "airline": "AM",
    "flightNumber": "AM680",
    "cabin": "ECONOMY",
    "baseFare": 180.00,
    "totalFare": 220.00,
    "currency": "USD",
    "seatsAvailable": 12
  }
]
```

`400 Bad Request` → if parameters are missing or `dateFrom > dateTo`.

---

## 🧩 What You Need to Implement for the Endpoint (already done here)

1) **Entity**: `FlightOffer` (JPA) — persisted in H2.  
2) **Repository**: `FlightOfferRepository` with the derived query:  
   ```java
   List<FlightOffer> findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureDateBetween(
       String origin, String destination, LocalDate dateFrom, LocalDate dateTo);
   ```
3) **Service**: `FlightSearchService#search(...)` validates inputs and queries the repository.  
4) **Controller**: `FlightSearchController#search(...)` exposes `/api/flights/search`.  
5) **Data**: `schema.sql` + `data.sql` preload the H2 database with sample routes/dates.  
6) **Actuator**: `/actuator/health` for basic health checks.  

All these pieces are already included in the source code of this project.

---

## 🗂️ Key Files

- `src/main/java/com/example/flightsearch/domain/FlightOffer.java`  
- `src/main/java/com/example/flightsearch/repository/FlightOfferRepository.java`  
- `src/main/java/com/example/flightsearch/service/FlightSearchService.java`  
- `src/main/java/com/example/flightsearch/web/FlightSearchController.java`  
- `src/main/resources/schema.sql`  
- `src/main/resources/data.sql`  
- `src/main/resources/application.yml`  
- `src/main/resources/flights_search_test.jmx` (JMeter plan)

---

## 📅 Default Test Data (H2)

These ranges are guaranteed to return results with the bundled sample data:

- **MEX → LAX**: `dateFrom=2025-12-20` — `dateTo=2025-12-28` (3 results: 20, 22, 26)  
- **GDL → LAX**: `2025-12-20` — `2025-12-22` (1 result: 21)  
- **MEX → JFK**: `2025-12-21` — `2025-12-24` (2 results: 21, 24)  
- **GRU → MIA**: `2025-12-20` — `2025-12-23` (2 results: 20, 23)

**Recommended default for demos:**  
```
origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28
```

---

## ▶️ How to Run (Local)

**From IntelliJ**  
- Open the project and run `FlightSearchApplication`.  
- Test:  
  - `http://localhost:8080/actuator/health`  
  - `http://localhost:8080/api/flights/search?origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28`

**From terminal**  
```bash
mvn -DskipTests clean package
java -jar target/flight-search-service-1.0.0.jar
```

---

## 🐳 Docker

Build & run with Compose (H2 is embedded in the service):
```bash
docker compose up --build
# or:
# docker build -t flight-search-service:1.0 .
# docker run --rm -p 8080:8080 flight-search-service:1.0
```

Test once the container is up:
```
http://localhost:8080/actuator/health
http://localhost:8080/api/flights/search?origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28
```

---

## 🔬 JMeter Test Plan

A ready-to-run JMeter plan is included at:  
`src/main/resources/flights_search_test.jmx`

It exercises the **search endpoint** with 10 virtual users over 100 iterations.  
Variables you can adjust inside the plan: `BASE_URL`, `ORIGIN`, `DEST`, `DATE_FROM`, `DATE_TO`.

**Run with JMeter GUI**: open the `.jmx` file and start the test.  
**Run in CLI**:
```bash
jmeter -n -t src/main/resources/flights_search_test.jmx -l results.jtl -e -o report
# open report/index.html
```

**Success criteria**  
- Response code **200** for all samples.  
- Non-empty JSON body.  
- Error rate **0%**.  
- Reasonable latency (e.g., p95 &lt; 100ms on a typical dev machine).

---

## 🛠️ H2 Console (Optional)

H2 console is enabled at `/h2-console` for local debugging.  
- JDBC URL: `jdbc:h2:mem:flightsdb`  
- User: `sa`  
- Password: `sa`

---

## ✅ Next Steps (for future labs)

- Externalize configuration with **Spring Cloud Config**.  
- Register services in **Eureka**.  
- Expose everything through **Spring Cloud Gateway**.  
- Add **Resilience4j** for timeouts/retries/fallbacks.  
- Add **Sleuth + Zipkin** for tracing.  
- Publish search events via **Spring Cloud Stream**.  
- Protect endpoints with **OAuth2/JWT** via **Spring Cloud Security**.  
- Define **Consumer-Driven Contracts** with **Spring Cloud Contract**.
