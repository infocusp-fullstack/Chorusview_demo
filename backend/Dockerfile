# Build from the repo root: docker build -f backend/Dockerfile .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build/backend
COPY backend/pom.xml .
COPY backend/src ./src
COPY proto /build/proto
RUN mkdir -p /build/envoy && mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/backend/target/trips-backend.jar app.jar
ENV GRPC_PORT=9090
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
