# Resilience Setup & JMeter Concurrency Test
This guide explains **where** to place each configuration (by service) to enable **Retry + TimeLimiter**, **Circuit Breaker**, and **Bulkhead** for the `flight-search-service` → `pricing-service` call. It also includes a **JMeter test plan** to generate concurrent load against `pricing-service`.

---

## 1) Architecture & Roles (who owns what)

- **pricing-service** (provider): Computes prices. *No resilience configuration here*. Optionally expose health and (for testing) a slow or error-prone mode.
- **flight-search-service** (consumer): Calls `pricing-service` to enrich flight results. **All Resilience4j (Retry/CB/TimeLimiter/Bulkhead) configurations live here.**
- **booking-service**: Persists bookings.

---

## 2) Dependencies (flight-search-service)

**pom.xml (flight-search-service)**

```xml
<!-- Spring Cloud Circuit Breaker with Resilience4j backend -->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>

<!-- Boot 3 integration for Resilience4j annotations & metrics -->
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>

<!-- Optional retry infrastructure -->
<dependency>
  <groupId>org.springframework.retry</groupId>
  <artifactId>spring-retry</artifactId>
</dependency>

<!-- Actuator for metrics -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Actuator exposure (flight-search-service `application.yml`)**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## 3) Retry + TimeLimiter (flight-search-service). Configurations

**`application.yml` (flight-search-service)**

```yaml
resilience4j:
  retry:
    instances:
      pricing:
        maxAttempts: 3                 # 1 try + 2 retries
        waitDuration: 200ms
        retryExceptions:
          - feign.RetryableException
          - java.io.IOException
          - org.springframework.web.client.ResourceAccessException
        ignoreExceptions:
          - org.springframework.web.client.HttpClientErrorException$BadRequest

  timelimiter:
    instances:
      pricing:
        timeoutDuration: 2s
        cancelRunningFuture: true

spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 1000      # ms
            read-timeout: 2000         # ms
```

**Facade code (flight-search-service)** — async method for TimeLimiter + Retry:

```java
@Retry(name = "pricing")
@io.github.resilience4j.timelimiter.annotation.TimeLimiter(name = "pricing")
public java.util.concurrent.CompletableFuture<PricingQuote> quoteAsync(java.math.BigDecimal baseFare, String currency, int bags,
                                                  String personType, int seatCount) {
  return java.util.concurrent.CompletableFuture.supplyAsync(() ->
      pricingClient.getQuote(baseFare, currency, bags, personType, seatCount), executor);
}
```

Use in service layer:
```java
var quote = pricingFacade.quoteAsync(baseFare, currency, 0, personType, seatCount)
  .exceptionally(ex -> fallbackQuote(baseFare, currency))
  .get();
```

---

## 4) Circuit Breaker (flight-search-service)

**`application.yml` (flight-search-service)**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      pricing:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

**Add annotation (flight-search-service)**

```java
@Retry(name = "pricing")
@io.github.resilience4j.timelimiter.annotation.TimeLimiter(name = "pricing")
@io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "pricing", fallbackMethod = "quoteFallback")
public java.util.concurrent.CompletableFuture<PricingQuote> quoteAsync(/* params */) { /* ... */ }

private java.util.concurrent.CompletableFuture<PricingQuote> quoteFallback(/* same params */, Throwable ex) {
  return java.util.concurrent.CompletableFuture.completedFuture(fallbackQuote(baseFare, currency));
}
```

---

## 5) Bulkhead (flight-search-service)

Choose one. For synchronous style, **SEMAPHORE**. For isolation with async, **THREADPOOL**.

**SEMAPHORE example (`application.yml`):**
```yaml
resilience4j:
  bulkhead:
    instances:
      pricing:
        maxConcurrentCalls: 20
        maxWaitDuration: 0
```

**THREADPOOL bulkhead example (`application.yml`):**
```yaml
resilience4j:
  thread-pool-bulkhead:
    instances:
      pricing:
        coreThreadPoolSize: 10
        maxThreadPoolSize: 20
        queueCapacity: 50
```

**Annotation:**
```java
@io.github.resilience4j.bulkhead.annotation.Bulkhead(
  name = "pricing",
  type = io.github.resilience4j.bulkhead.Bulkhead.Type.SEMAPHORE // or THREADPOOL
)
```

---

## 6) Provider-side (pricing-service) notes

**No resilience config required** here. For testing purposes you may add:
- `GET /api/pricing/slow` to simulate delay (e.g., `Thread.sleep(3000)` → triggers TimeLimiter).
- Random 5xx responses to trigger retries.
- Ensure Actuator health is enabled for observability.

---

## 7) JMeter Concurrency Test Plan (pricing-service)

### 7.1 What we’ll test
- **Endpoint**: `GET /api/pricing/quote`
- **Example URL**: `http://localhost:9090/api/pricing/quote?baseFare=100&currency=USD&bags=0&personType=ADULT&seatCount=2`
- **Goal**: Simulate **concurrent requests** (e.g., 50–200 threads) to observe latency, error rates, TimeLimiter timeouts, and Bulkhead rejections when called through `flight-search-service` or directly.

### 7.2 Provided JMX
- **File**: `pricing-concurrency-test.jmx`
- Includes:
  - **Thread Group**: configurable users & ramp-up
  - **HTTP Request Defaults**: host/port/path base
  - **HTTP Request** sampler for `/api/pricing/quote`
  - **View Results in Table** + **Summary Report** listeners

### 7.3 How to run
1. Start **pricing-service** (and optionally **flight-search-service** if you want to hit it instead).
2. Open **Apache JMeter**.
3. `File > Open > pricing-concurrency-test.jmx`.
4. In **HTTP Request Defaults**, set:
   - Pricing service or Flight search URL
5. In **Thread Group**:
   - Users: `100`
   - Ramp-up: `20` seconds
   - Loop Count: set **Duration** to `60` seconds.
6. Run the test; watch **Summary Report** for throughput, avg latency, error %, and check service logs for CB/Retry/TimeLimiter/Bulkhead behavior.
7. Try variations:
   - Increase users to `200+` to force **Bulkhead** rejections.
   - Add artificial delays/errors in `pricing-service` to trigger **Retry** and **Circuit Breaker**.

---

## 8) Observability checklist (flight-search-service)
- Metrics:
  - `resilience4j.circuitbreaker.calls{name="pricing",state="successful|failed|not_permitted"}`
  - `resilience4j.retry.calls{name="pricing",outcome="successful_with_retry|failed_with_retry"}`
  - `resilience4j.timelimiter.calls{name="pricing",outcome="timeout|success"}`
  - `resilience4j.bulkhead.calls{name="pricing",kind="permitted|rejected"}`
- Logs:
  - Log fallback paths and short exception messages.

---

## 9) Expected outcomes
- With **Retry + TimeLimiter**, transient failures get retried; long waits are capped.
- With **Circuit Breaker**, sustained failures trip the breaker → fast fallbacks until provider recovers.
- With **Bulkhead**, concurrency spikes are limited; excess calls are rejected quickly to protect the service.

