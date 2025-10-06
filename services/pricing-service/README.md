# pricing-service

A minimal Spring Boot service that calculates flight pricing **without** DB, Eureka, or Config Client.

## Build & Run

```
# From project root
./mvnw spring-boot:run || mvn spring-boot:run
```

Service starts on **http://localhost:8081**.

### Example request
```
curl "http://localhost:8081/api/pricing/quote?baseFare=100&currency=USD&bags=2"
```

### Example response
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

## Docker (optional)
```
# Build fat JAR
./mvnw -DskipTests clean package || mvn -DskipTests clean package

# Build image
docker build -t pricing-service:1.0.0 .

# Run
docker run --rm -p 8081:8081 pricing-service:1.0.0
```

## How to call from flight-search-service

1) Add a config property for the base URL (e.g. `pricing.base-url=http://localhost:8081`).
2) Use **WebClient** or **RestTemplate** to call:
   - `GET /api/pricing/quote?baseFare={baseFare}&currency={currency}&bags={bags}`

### WebClient snippet (recommended)
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

Call this from the service method that builds your flight search response to enrich each flight with `tax`, `bagFees`, and `totalFare`.
