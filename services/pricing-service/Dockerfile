FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/pricing-service-*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java","-jar","/app/app.jar"]
