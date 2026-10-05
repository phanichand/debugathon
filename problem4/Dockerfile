FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/build/libs/platform.jar /app/platform.jar
USER 10001
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/platform.jar"]
