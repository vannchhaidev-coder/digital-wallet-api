FROM gradle:8.10-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle ./
COPY gradle ./gradle
COPY api-gateway ./api-gateway
RUN gradle :api-gateway:build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
