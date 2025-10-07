# Spring Cloud Security Integration with Keycloak (Local Lab Setup)

This guide explains how to integrate **Spring Cloud Gateway** and **microservices** with **Keycloak** for authentication and JWT validation.  
It assumes Keycloak and its PostgreSQL database are already included in your `docker-compose.yml` file.

---

## Keycloak Setup via UI

Once Keycloak is running (`http://localhost:8099`):

### Step 1 — Access the Admin Console
- Navigate to: [http://localhost:8099](http://localhost:8099)
- Log in with the admin credentials defined in `docker-compose.yml`:  
  `admin / admin`

### Step 2 — Create Realm
- Click **Add Realm** → name it `travel`.

### Step 3 — Create a Client
- Inside the `travel` realm:
  - Go to **Clients → Create**.
  - **Client ID**: `spring-gateway`
  - **Client Type**: Public (for local dev)
  - **Valid Redirect URIs**: `*`
  - **Direct Access Grants**: Enabled
  - Save and enable **Standard Flow**.

### Step 4 — Add Roles and Users
- Create roles: `user`, `admin`
- Create a test user:
  - Username: `john`
  - Password: `password`
  - Assign the role `user` under **Role Mappings**.

**Issuer URI:**  
`http://localhost:8099/realms/travel`

**JWKS URI:**  
`http://localhost:8099/realms/travel/protocol/openid-connect/certs`

---

## Gateway Configuration

### Dependencies (Maven)

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
</dependencies>
```

### `application.yml` (Gateway)

```yaml
server:
  port: 8088
  
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      # Enable service discovery (Eureka) integration at the gateway
      discovery:
        locator:
          enabled: true
          lowerCaseServiceId: true

      httpclient:
        connect-timeout: 1000       # ms
        responseTimeout: 3s         # global response timeout

      routes:
        # ----------------- Flights Route -----------------
        - id: flights
          uri: lb://flight-search-service         # Use load-balanced discovery to route to the service
          predicates:
            - Path=/api/flights/**                # Match incoming requests starting with /api/flights/
          filters:
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
            connect-timeout: 800     # ms
            response-timeout: 2500   # ms

        # ----------------- Bookings Route -----------------
        - id: bookings
          uri: lb://booking-service
          predicates:
            - Path=/api/bookings/**
          filters:
            - name: Retry
              args:
                retries: 1
                methods: POST,GET
                statuses: BAD_GATEWAY, GATEWAY_TIMEOUT, INTERNAL_SERVER_ERROR
            - name: CircuitBreaker
              args:
                name: bookingsEdgeCB
                fallbackUri: forward:/fallback/bookings
          metadata:
            connect-timeout: 1000    # ms
            response-timeout: 5000   # ms (bookings may take longer)

        # ----------------- Pricing Route -----------------
        - id: pricing
          uri: lb://pricing-service
          predicates:
            - Path=/api/pricing/**
          filters:
            - name: Retry
              args:
                retries: 1
                methods: GET
                statuses: BAD_GATEWAY, GATEWAY_TIMEOUT, INTERNAL_SERVER_ERROR
            - name: CircuitBreaker
              args:
                name: pricingEdgeCB
                fallbackUri: forward:/fallback/pricing
          metadata:
            connect-timeout: 600     # ms
            response-timeout: 2000   # ms (aggressive for pricing)

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8099/realms/travel

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,gateway,refresh,env
```

### `SecurityConfig.java` (Gateway - WebFlux)

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      .authorizeExchange(auth -> auth
        .pathMatchers("/actuator/**").permitAll()
        .anyExchange().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2.jwt())
      .build();
  }
}
```

---

## Microservice Example — Flight Search Service

### Dependencies

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
  </dependency>
</dependencies>
```

### `application.yml` (Flight Search)

```yaml
server:
  port: 8080

spring:
  application:
    name: flight-search-service

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8099/realms/travel
```

### `SecurityConfig.java` (Servlet stack)

```java
@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/**").permitAll()
        .requestMatchers("/api/flights/**").hasRole("user")
        .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2.jwt());
    return http.build();
  }
}
```
---

## Testing with Keycloak Token

### Obtain JWT token

```bash
curl -X POST "http://localhost:8099/realms/travel/protocol/openid-connect/token"   -H "Content-Type: application/x-www-form-urlencoded"   -d "client_id=spring-gateway"   -d "grant_type=password"   -d "username=john"   -d "password=password"
```

Copy the `access_token` and test:

```
TOKEN="<paste_token_here>"
curl -H "Authorization: Bearer $TOKEN"   "http://localhost:8088/api/flights/search"
```
---

## Useful Notes

- The `Authorization` header is **propagated automatically** from Gateway to downstream services.
- Each microservice **validates the same JWT** using the Keycloak issuer.
- To secure all traffic:
  - Use HTTPS in production (Traefik/Nginx as reverse proxy).
  - Store Keycloak secrets in a secret manager.
- For local dev, `issuer-uri` must exactly match the token’s `iss` claim:
  ```
  "iss": "http://localhost:8099/realms/travel"
  ```

---

**Result:**  
You now have a complete local environment with Keycloak-based authentication, Spring Cloud Gateway JWT validation, and secured microservices (example: Flight Search).
