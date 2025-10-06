# Eureka Server Setup + OpenFeign Integration (Flight Search Service)

This guide walks you through:
1) Setting up a **Spring Cloud Netflix Eureka Server** locally.  
2) Registering services (e.g., `pricing-service`, `flight-search-service`) as Eureka clients.  
3) Integrating **OpenFeign** in `flight-search-service` to call `pricing-service` using service discovery.

> Prereqs: Java 17+, Maven 3.9+, Spring Boot 3.x, Spring Cloud 2023.x (Ilford).

---

## 1) Create the Eureka Server

### 1.1 `pom.xml`
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>eureka-server</artifactId>
  <version>1.0.0</version>

  <properties>
    <java.version>17</java.version>
    <spring.boot.version>3.3.3</spring.boot.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>2023.0.3</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring.boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-logging</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

### 1.2 Main Application
```java
// src/main/java/com/example/eureka/EurekaServerApplication.java
package com.example.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(EurekaServerApplication.class, args);
  }
}
```

### 1.3 Configuration
```yaml
# src/main/resources/application.yml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

### 1.4 Run & Verify
```
mvn spring-boot:run
```
Open the **Eureka Dashboard**: <http://localhost:8761>  
You should see a status page; initially there will be **no registered instances**.

---

## 2) Register Services as Eureka Clients

### 2.1 Common Client Dependency
For each client service (e.g., `pricing-service`, `flight-search-service`), add:
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### 2.2 Client Configuration
Example (`pricing-service`):
```yaml
spring:
  application:
    name: pricing-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

Repeat similarly for `flight-search-service`:
```yaml
spring:
  application:
    name: flight-search-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

Start each service and confirm they appear on the Eureka dashboard.

---

## 3) OpenFeign Integration in flight-search-service

### 3.1 Dependencies
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### 3.2 Enable Feign
```java
// src/main/java/com/example/flightsearch/FlightSearchApplication.java
package com.example.flightsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class FlightSearchApplication {
  public static void main(String[] args) {
    SpringApplication.run(FlightSearchApplication.class, args);
  }
}
```

### 3.3 Declare the Feign Client (calls pricing-service)
```java
// src/main/java/com/example/flightsearch/pricing/PricingClient.java
package com.example.flightsearch.pricing;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "pricing-service") // must match provider's spring.application.name
public interface PricingClient {

  @GetMapping("/api/pricing/quote")
  PricingQuote getQuote(@RequestParam BigDecimal baseFare,
                        @RequestParam String currency,
                        @RequestParam int bags);
}
```

```java
// src/main/java/com/example/flightsearch/pricing/PricingQuote.java
package com.example.flightsearch.pricing;

import java.math.BigDecimal;

public record PricingQuote(
    BigDecimal baseFare,
    BigDecimal tax,
    BigDecimal bagFees,
    BigDecimal totalFare,
    String currency,
    String rulesVersion
) {}
```

### 3.4 Optional: Feign Timeouts / Logging
```yaml
# flight-search-service/src/main/resources/application.yml (add)
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 1000
            read-timeout: 2000
            logger-level: basic

logging:
  level:
    com.example.flightsearch.pricing.PricingClient: DEBUG
```

### 3.5 Use the Feign Client in Your Service Layer
```java
// src/main/java/com/example/flightsearch/service/FlightSearchService.java
package com.example.flightsearch.service;

import com.example.flightsearch.pricing.PricingClient;
import com.example.flightsearch.pricing.PricingQuote;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FlightSearchService {

  private final FlightRepository flightRepository;
  private final PricingClient pricingClient;

  public FlightSearchService(FlightRepository flightRepository, PricingClient pricingClient) {
    this.flightRepository = flightRepository;
    this.pricingClient = pricingClient;
  }

  public List<FlightDto> search(FlightQuery q) {
    var flights = flightRepository.search(q.origin(), q.destination(), q.dateFrom(), q.dateTo());
    return flights.stream().map(f -> {
      try {
        PricingQuote quote = pricingClient.getQuote(f.getBaseFare(), f.getCurrency(), 0);
        return FlightDto.from(f)
            .withTax(quote.tax())
            .withBagFees(quote.bagFees())
            .withTotalFare(quote.totalFare())
            .withCurrency(quote.currency());
      } catch (Exception ex) {
        // Fallback — return base fare if provider is unavailable
        return FlightDto.from(f)
            .withTax(BigDecimal.ZERO)
            .withBagFees(BigDecimal.ZERO)
            .withTotalFare(f.getBaseFare())
            .withCurrency(f.getCurrency());
      }
    }).toList();
  }
}
```

---

## 4) Run & Verify End-to-End

1. **Start Eureka Server**:
   ```
   cd eureka-server
   mvn spring-boot:run
   # open http://localhost:8761
   ```

2. **Start pricing-service** and confirm it registers in Eureka.

3. **Start flight-search-service** and confirm it registers in Eureka.

4. **Exercise the flow**:
   ```
   curl "http://localhost:8080/api/flights/search?origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28"
   ```
   Each returned flight should include pricing fields (`tax`, `bagFees`, `totalFare`).

You now have a local **Eureka Server** and a **flight-search-service** that calls `pricing-service` via **OpenFeign** and **Eureka service discovery**.
