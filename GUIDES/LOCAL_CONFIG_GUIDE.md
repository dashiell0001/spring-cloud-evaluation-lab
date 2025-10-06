# 🛠 Guide: Running Config Server and Flight Search Service with Postgres DB

This guide describes how to:
1. Run Postgres DB via Docker Compose.  
2. Configure and start a Spring Cloud Config Server on port **8090** using local filesystem configs.  
3. Verify that Config Server serves configurations.  
4. Configure and run Flight Search Service on port **8080**, pointing to both the Postgres DB and Config Server.

---

## 1. Run Postgres DB with Docker Compose

Create a file named `docker-compose-db.yml`:

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

Start the database:

```
docker compose -f docker-compose-db.yml up -d
```

Verify the container:

```
docker ps
```

---

## 2. Configure Config Server (port 8090, local filesystem)

Inside your `config-service` project, configure **`application.yml`**:

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
          search-locations: "file:///ABSOLUTE/PATH/TO/config-repo"
```

> Replace with the **absolute path** to your local `config-repo` folder.  
> Example: `file:///Users/you/dev/config-repo`

---

## 3. Prepare Configurations

Inside your `config-repo/configs/` folder, create files:

```
config-repo/
└── configs/
    ├── flight-search-service.yml
    ├── flight-search-service-dev.yml
```

**flight-search-service.yml**

```yaml
server:
  port: 8080

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
        include: health,info
```

---

## 4. Start Config Server

Run:

```
cd config-service
mvn spring-boot:run
```

Verify Config Server is serving configurations:

```
curl http://localhost:8090/flight-search-service/default
curl http://localhost:8090/flight-search-service/dev
```

You should see JSON with property sources.

---

## 5. Configure Flight Search Service

Add Config Client dependency to `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

Update **`application.yml`** for the Flight Search Service:

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
      fail-fast: true  # Fail startup if Config Server is not available

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

## 6. Run Flight Search Service

From project root:

```
mvn spring-boot:run
```

If Config Server is **up and serving configs**, the service starts and connects to the Postgres DB started with Docker Compose.

If Config Server is **down**, the service **fails to start** because `fail-fast: true` is enabled.

---

## 7. Verify Service

Test the health endpoint:

```
curl http://localhost:8080/actuator/health
```

Test the search endpoint:

```
curl "http://localhost:8080/api/flights/search?origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28"
```