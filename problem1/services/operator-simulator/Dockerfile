FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :services:operator-simulator:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/operator-simulator/build/libs/app.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
