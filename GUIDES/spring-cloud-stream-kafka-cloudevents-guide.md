# Spring Cloud Stream + Kafka with CloudEvents (Booking → Notification) + Consumer Security

This guide shows how to:
- Enable **Kafka** in your existing Docker Compose.
- Publish **CloudEvents** from `booking-service` using **Spring Cloud Stream**.
- Consume those events in a new **notification-service** (Spring Boot 3) and **validate a JWT** embedded in the event headers (consumer-side security).
- Log a dummy “email sent” message with booking data (no external provider).

> Scope: local dev. Kafka is unauthenticated at the broker; we validate **application-level security** on the **consumer** by decoding a JWT (issued by your Keycloak) embedded in CloudEvent headers.

---

## 1) Kafka: docker-compose

Kafka should be enabled as part of existing docker compose.

```yaml
services:
  kafka:
    image: docker.redpanda.com/redpandadata/redpanda:v23.3.10
    command:
      - redpanda start
      - --overprovisioned
      - --smp=1
      - --memory=1G
      - --reserve-memory=0M
      - --kafka-addr=PLAINTEXT://0.0.0.0:9092
      - --advertise-kafka-addr=PLAINTEXT://kafka:9092
    ports:
      - "9092:9092"    # Kafka endpoint
      - "9644:9644"    # Admin (optional)
    restart: unless-stopped
```

If your apps run **on the host**, use `localhost:9092`. If they run **in Compose**, use `kafka:9092`.

---

## 2) Event contract (shared DTO)

Create a class shared by producer and consumer (or copy the class in each project):

```java
// common/dto/BookingCreatedEvent.java
package com.example.events;

import java.time.Instant;
import java.util.List;

public record BookingCreatedEvent(
    String bookingId,
    String userEmail,
    String flightNumber,
    String origin,
    String destination,
    String departureDate, // yyyy-MM-dd
    List<String> seats,
    Instant createdAt
) {}
```

---

## 3) Booking Service — Producer (Spring Cloud Stream + CloudEvents)

### 3.1 Maven dependencies
```xml
<dependencies>
    <dependencies>
        <!-- Cloud Stream + Kafka binder -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-stream-kafka</artifactId>
        </dependency>

        <!-- CloudEvents API integration -->
        <dependency>
            <groupId>io.cloudevents</groupId>
            <artifactId>cloudevents-core</artifactId>
            <version>2.5.0</version>
        </dependency>
        <dependency>
            <groupId>io.cloudevents</groupId>
            <artifactId>cloudevents-spring</artifactId>
            <version>2.5.0</version>
        </dependency>
        <dependency>
            <groupId>io.cloudevents</groupId>
            <artifactId>cloudevents-json-jackson</artifactId>
            <version>2.5.0</version>
        </dependency>
    </dependencies>
</dependencies>
```

### 3.2 application.yml
> Use `StreamBridge` to send to the binding `bookingEvents-out-0` with **CloudEvents** headers.

```yaml
spring:
  application:
    name: booking-service

  cloud:
    stream:
      kafka:
        binder:
          brokers: localhost:9092
          autoCreateTopics: true

      bindings:
        bookingEvents-out-0:
          destination: booking.events
          content-type: application/cloudevents+json   # ← CloudEvents
          producer:
            useNativeEncoding: true
            partition-key-expression: headers['ce-id']
            errorChannelEnabled: true           

        # Producer DLQ
        producerDlq-out-0:
          destination: booking.events.producer.dlq
          content-type: application/json
          producer:
            useNativeEncoding: true
```

### 3.3 Publisher using `StreamBridge` (adds CloudEvent headers)
```java
import com.example.events.BookingCreatedEvent;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.spring.messaging.CloudEventMessageUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class BookingEventPublisher {
    
    private final StreamBridge streamBridge;
    
    public BookingEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }
    
    public void publish(BookingCreatedEvent booking, String bearerJwt) {
        CloudEvent event = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType("com.example.booking.created")
                .withSource(URI.create("urn:booking-service"))
                .withTime(OffsetDateTime.now())
                .withData("application/json",
                        toJson(booking).getBytes(StandardCharsets.UTF_8))
                .build();
        
        Message<CloudEvent> message = MessageBuilder.withPayload(event)
                .copyHeaders(CloudEventMessageUtils.toMap(event))
                .setHeader("ce-authorization", bearerJwt)
                .build();
        
        boolean sent = streamBridge.send("bookingEvents-out-0", message);
        if (!sent) {
            throw new IllegalStateException("Failed to publish CloudEvent for booking " + booking.bookingId());
        }
    }
    
    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

Call `publish(event, "Bearer <JWT>")` right after persisting a booking (e.g., in your `BookingService`). You can obtain the user token from the HTTP request or use a service token (Keycloak client credentials).

---

#### Error handler
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class ProducerDlqHandler {
    private static final Logger log = LoggerFactory.getLogger(ProducerDlqHandler.class);
    private final StreamBridge streamBridge;
    
    public ProducerDlqHandler(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }
    
    @ServiceActivator(inputChannel = "bookingEvents-out-0.errors")
    public void onPublishError(Message<?> errorMessage) {
        log.error("Error publishing event to booking.events; sending to DLQ.");
        Object original = errorMessage.getHeaders().get("originalMessage");
        if (original instanceof Message<?> originalMsg) {
            streamBridge.send("producerDlq-out-0", originalMsg);
        }
    }
}
```

#### Token extraction

```java
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TokenService {

  /**
   * @return Optional "Bearer <jwt>" if exists
   */
  public Optional<String> currentBearer() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return Optional.empty();

    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      return Optional.of("Bearer " + jwt.getTokenValue());
    }

    Object principal = auth.getPrincipal();
    if (principal instanceof Jwt jwt) {
      return Optional.of("Bearer " + jwt.getTokenValue());
    }

    return Optional.empty();
  }
  
  public String requireCurrentBearer() {
    return currentBearer().orElseThrow(() ->
        new IllegalStateException("No JWT present in SecurityContext"));
  }
}
```

---

## 4) Notification Service — Consumer (Spring Cloud Stream + CloudEvents + JWT Validation)

Below is a minimal **complete project skeleton** you can paste into a new module `notification-service`.

### 4.1 `pom.xml`
```xml
<!-- Spring Cloud Stream + Kafka -->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-stream-kafka</artifactId>
</dependency>

<!-- Security: decode/validate JWT in the consumer -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
  
<!-- Cloud events -->
<dependency>
  <groupId>io.cloudevents</groupId>
  <artifactId>cloudevents-spring</artifactId>
  <version>2.5.0</version>
</dependency>
<dependency>
  <groupId>io.cloudevents</groupId>
  <artifactId>cloudevents-json-jackson</artifactId>
  <version>2.5.0</version>
</dependency>
```

### 4.2 `src/main/resources/application.yml`
```yaml
server:
  port: 8084

spring:
  application:
    name: notification-service

  cloud:
    function:
      definition: bookingCreatedConsumer
    stream:
      kafka:
        binder:
          brokers: localhost:9092
          autoCreateTopics: true         
      bindings:
        bookingCreatedConsumer-in-0:
          destination: booking.events
          group: notifications
          content-type: application/cloudevents+json
          consumer:
            max-attempts: 3
            enableDlq: true               # ← DLQ
            dlqName: booking.events.dlq  
            autoCommitOffset: true

# JWT Validation
security:
  jwt:
    issuer-uri: http://localhost:8099/realms/travel

logging.level.org.springframework.cloud.stream: INFO
```

### 4.3 Consumer with CloudEvents headers + JWT validation
```java
import com.example.events.BookingCreatedEvent;
import io.cloudevents.CloudEvent;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

import java.util.function.Consumer;

@Configuration
public class BookingCreatedConsumerConfig {
    
    private static final Logger log = LoggerFactory.getLogger(BookingCreatedConsumerConfig.class);
    private final JwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation("http://localhost:8099/realms/travel");
    
    @Bean
    public Consumer<Message<CloudEvent>> bookingCreatedConsumer() {
        return message -> {
            CloudEvent event = message.getPayload();
            String bearer = (String) message.getHeaders().get("ce-authorization");
            
            if (bearer != null && bearer.startsWith("Bearer ")) {
                String token = bearer.substring(7);
                Jwt jwt = jwtDecoder.decode(token);
                log.info("JWT validated for subject={} (event id={})", jwt.getSubject(), event.getId());
            } else {
                log.warn("Missing ce-authorization header; message discarded");
                return;
            }

            if (event.getData() instanceof JsonCloudEventData data) {
                try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper().registerModule(JsonFormat.getCloudEventJacksonModule());
                    BookingCreatedEvent booking = mapper.readValue(data.toBytes(), BookingCreatedEvent.class);
                    log.info("Sending booking email to {} for flight {} {}→{} on {}", booking.userEmail(), booking.flightNumber(), booking.origin(), booking.destination(), booking.departureDate());
                } catch (Exception e) {
                    log.error("Error parsing CloudEvent data", e);
                }
            }
        };
    }
}
```

---

## 5) Testing

1) **Start compose**: `docker compose up -d`
2) **Ensure Keycloak is running** (`http://localhost:8099`) and obtain a token:
   ```
   curl -X POST "http://localhost:8099/realms/travel/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=spring-gateway" -d "grant_type=password" -d "username=john" -d "password=password"
   ```
3) **Run notification-service**: `mvn -q -f notification-service spring-boot:run`
4) **Publish** from booking-service with `ce-authorization: Bearer <access_token>`
5) **Observe logs** in notification-service: JWT validation + “Sending booking email …”

