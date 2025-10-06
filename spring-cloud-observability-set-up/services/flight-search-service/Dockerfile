# Stage 1: build (JDK 17)
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY . .
# Usa Maven Wrapper si existe; si no, usa mvn del entorno
RUN ./mvnw -DskipTests clean package || mvn -DskipTests clean package

# Stage 2: runtime (JRE 17)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# User no-root (buena práctica)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy JAR generated at build stage
COPY --from=build /workspace/target/flight-search-service-*.jar app.jar

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java","-jar","/app/app.jar"]
