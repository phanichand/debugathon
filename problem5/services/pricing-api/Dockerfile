FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 pricing
COPY --from=build /build/target/pricing-api-1.0.0.jar /app/service.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/service.jar"]
