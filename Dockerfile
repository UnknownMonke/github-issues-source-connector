# Dockerfile for production-ready deployment.

FROM gradle:9.2.0-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle /app/
COPY src /app/src
COPY gradle /app/gradle
RUN gradle shadowJar

# Provides a Connect distributed worker with configuration listed in environment.
FROM confluentinc/cp-kafka-connect:latest
# Creates dirs.
RUN mkdir -p /opt/kafka/plugins
RUN mkdir -p /tmp/configs
# Copies connector JAR and example configs into plugin path.
COPY --from=build /app/build/libs/*.jar /opt/kafka/plugins/
COPY config /tmp/configs
