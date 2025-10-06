# Guide: Spring Cloud Config Server (Native) + Flight Search Service Client

This guide explains how to:

1. Configure and run a **Spring Cloud Config Server** using the **native filesystem backend**.  
2. Connect the **Flight Search Service** client to the Config Server.  
3. **Refresh properties** dynamically without restarting the client.  
4. Use **Spring Boot Actuator** to list and inspect configurations.

---

## 1) Config Server (Native Mode)

### 1.1 Dependencies (pom.xml)
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>2023.0.3</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
</dependencies>
```

### 1.2 Enable the Config Server
```java
@EnableConfigServer
@SpringBootApplication
public class ConfigServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(ConfigServiceApplication.class, args);
  }
}
```

### 1.3 Configuration repository (filesystem)
Create a folder for configs:

```
/absolute/path/config-repo/
└─ configs/
   ├─ flight-search-service.yml
   ├─ flight-search-service-dev.yml
   └─ flight-search-service-prod.yml
```

Example **flight-search-service.yml**:
```yaml
server:
  port: 8080

app:
  message: "Hello from Config Server (default) 👋"

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flights
    username: flight
    password: flight
  jpa:
    hibernate:
      ddl-auto: none

management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh,env,configprops
```

### 1.4 Config Server application.yml
```yaml
server:
  port: 8090

spring:
  application:
    name: config-service
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: "file:///absolute/path/config-repo/configs"
```

### 1.5 Run and test the Config Server
```bash
mvn spring-boot:run
```

Test:
```bash
curl http://localhost:8090/flight-search-service/default
```

---

## 2) Flight Search Service Client

### 2.1 Dependencies (pom.xml)
```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
  </dependency>
</dependencies>
```

### 2.2 Client application.yml
```yaml
server:
  port: 8080

spring:
  application:
    name: flight-search-service
  profiles:
    active: default
  config:
    import: "configserver:http://localhost:8090"
  cloud:
    config:
      fail-fast: true
      retry:
        max-attempts: 6
        initial-interval: 1000
        multiplier: 1.5
        max-interval: 5000

management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh,env,configprops
```

### 2.3 Example: refreshable property
```java
@RefreshScope
@Component
public class MessageProps {
  @Value("${app.message:Hello (local default)}")
  private String message;
  public String getMessage() { return message; }
}

@RestController
public class ConfigController {
  private final MessageProps props;
  public ConfigController(MessageProps props) { this.props = props; }
  @GetMapping("/api/config/message")
  public String message() { return props.getMessage(); }
}
```

---

## 3) Run Database (Postgres via Docker Compose)

docker-compose-db.yml:
```yaml
version: "3.9"
services:
  db:
    image: postgres:16-alpine
    container_name: flights-postgres
    environment:
      POSTGRES_DB: flights
      POSTGRES_USER: flight
      POSTGRES_PASSWORD: flight
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
volumes:
  pgdata:
```

Run:
```bash
docker compose -f docker-compose-db.yml up -d
```

---

## 4) Start Client and Verify

1. Ensure Config Server is running on port 8090.  
2. Start client:
   ```bash
   mvn spring-boot:run
   ```
   Logs should show: `Located property source: [configserver:...]`.

3. Test:
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8080/api/config/message
   ```

If Config Server is down, startup will fail (`fail-fast: true`).

---

## 5) Refresh properties dynamically

1. Edit **flight-search-service.yml**:
```yaml
app:
  message: "Hello (UPDATED) from local repo 🚀"
```

2. Trigger refresh:
```bash
curl -X POST http://localhost:8080/actuator/refresh
```

3. Recheck:
```bash
curl http://localhost:8080/api/config/message
# -> "Hello (UPDATED) from local repo 🚀"
```

---

## 6) Inspect configurations with Actuator

- **/actuator/env** → shows active environment and property sources
```bash
curl http://localhost:8080/actuator/env
```

- **/actuator/configprops** → shows beans bound to configuration properties
```bash
curl http://localhost:8080/actuator/configprops
```

- **/actuator/health** → health status  
- **/actuator/info** → optional info endpoint

---

✅ With this setup you have:  
- A **Config Server** running locally with native filesystem configs.  
- A **Flight Search Service** client strictly dependent on the Config Server.  
- Ability to **refresh properties dynamically**.  
- Tools to **inspect configuration sources** via Actuator.
