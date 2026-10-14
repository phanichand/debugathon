FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
ARG SERVICE
RUN ./gradlew :services:${SERVICE}:bootJar --no-daemon && cp services/${SERVICE}/build/libs/app.jar /app.jar

FROM curlimages/curl:8.12.1 AS agent
RUN curl -fsSL https://repo.maven.apache.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/2.20.1/opentelemetry-javaagent-2.20.1.jar -o /tmp/agent.jar \
    && echo 'd94e9fc648cab37e540571352937acc06b8014182ddbd2d93a6c08641c84e48e  /tmp/agent.jar' | sha256sum -c -

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app.jar app.jar
COPY --from=agent /tmp/agent.jar /otel/agent.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
