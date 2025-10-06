# Pricing Service — Setup & Verification Guide

A minimal Spring Boot microservice that calculates pricing without DB, Eureka, or Config Client.

## 1) Prerequisites
- Java 17+
- Maven 3.9+ (or use the provided `mvnw`/`mvnw.cmd`)
- cURL (for quick tests)
- (Optional) Docker

## 2) Project Structure
```
pricing-service/
  ├─ pom.xml
  ├─ src/main/java/com/example/pricing/PricingServiceApplication.java
  ├─ src/main/java/com/example/pricing/controller/PricingController.java
  ├─ src/main/java/com/example/pricing/model/PricingQuote.java
  ├─ src/main/resources/application.yml
  ├─ Dockerfile
  └─ README.md
```

## 3) Build & Run (Local JVM)

From the project root:

```
# Option A: Use wrapper (preferred)
./mvnw spring-boot:run

# Option B: Use your Maven
mvn spring-boot:run
```

The service will start at **http://localhost:9090**.

### 3.1 Health Check
```
curl http://localhost:9090/actuator/health
```

Expected:
```json
{"status":"UP"}
```

## 4) Usage — Pricing Quote API

**Endpoint**
```
GET /api/pricing/quote?baseFare={decimal}&currency={string}&bags={int}
```

**Example**
```
curl "http://localhost:9090/api/pricing/quote?baseFare=100&currency=USD&bags=2"
```

**Response**
```json
{
  "baseFare": 100.00,
  "tax": 21.00,
  "bagFees": 60.00,
  "totalFare": 181.00,
  "currency": "USD",
  "rulesVersion": "v1"
}
```

**Rules**
- `tax = baseFare * 0.21`
- `bagFees = 30.00 * bags`
- `totalFare = baseFare + tax + bagFees`
- Amounts are rounded to 2 decimals (HALF_UP).

**Validation**
- `baseFare`: required, decimal.
- `currency`: required, non-empty (case-insensitive).
- `bags`: integer `>= 0`.

## 5) Run with Docker (Optional)

### 5.1 Build JAR
```
./mvnw -DskipTests clean package
# Result: target/pricing-service-1.0.0.jar
```

### 5.2 Build Image
```
docker build -t pricing-service:1.0.0 .
```

### 5.3 Run Container
```
docker run --rm -p 9090:9090 pricing-service:1.0.0
```

### 5.4 Test
```
curl "http://localhost:9090/api/pricing/quote?baseFare=120&currency=USD&bags=1"
```

## 6) Integrating from Flight Search Service

1. Add a property for the base URL:
   ```yaml
   pricing:
     base-url: http://localhost:9090
   ```
   or environment variable `PRICING_BASE_URL=http://localhost:9090`.

2. Use **WebClient** or **RestTemplate** to call:
   ```http
   GET /api/pricing/quote?baseFare={baseFare}&currency={currency}&bags={bags}
   ```

### WebClient Snippet
```java
@Bean
WebClient pricingWebClient(WebClient.Builder builder,
                           @Value("${pricing.base-url}") String baseUrl) {
  return builder.baseUrl(baseUrl).build();
}

record PricingQuote(BigDecimal baseFare, BigDecimal tax, BigDecimal bagFees,
                    BigDecimal totalFare, String currency, String rulesVersion) {}

public PricingQuote getQuote(WebClient pricingWebClient,
                             BigDecimal baseFare, String currency, int bags) {
  return pricingWebClient.get()
    .uri(uriBuilder -> uriBuilder.path("/api/pricing/quote")
      .queryParam("baseFare", baseFare)
      .queryParam("currency", currency)
      .queryParam("bags", bags)
      .build())
    .retrieve()
    .bodyToMono(PricingQuote.class)
    .block();
}
```

Call this inside the service method that assembles flight results to enrich each item with `tax`, `bagFees`, and `totalFare`. Handle failures gracefully (fallback to `baseFare` if needed).

## 7) Troubleshooting

- **Port already in use (9090):** Stop the other process or change `server.port` in `application.yml`.
- **HTTP 400 (Bad Request):** Check required params: `baseFare`, `currency`, `bags >= 0`.
- **Validation errors:** The service uses Jakarta Bean Validation; verify parameter types/values.
- **Docker build fails:** Ensure the JAR exists (`target/pricing-service-*.jar`) before building the image.

## 8) Quick Test Matrix

| Case                      | Request                                                                 | Expected totalFare |
|--------------------------|-------------------------------------------------------------------------|--------------------|
| No bags                  | `/api/pricing/quote?baseFare=100&currency=USD&bags=0`                   | 121.00             |
| One bag                  | `/api/pricing/quote?baseFare=100&currency=USD&bags=1`                   | 151.00             |
| Two bags                 | `/api/pricing/quote?baseFare=100&currency=USD&bags=2`                   | 181.00             |
| Different currency tag   | `/api/pricing/quote?baseFare=80.5&currency=eur&bags=1`                  | 127.41             |
| Large base fare          | `/api/pricing/quote?baseFare=1000&currency=USD&bags=0`                  | 1210.00            |

> Note: Currency is echoed back uppercased; there is no FX conversion in this lab.
