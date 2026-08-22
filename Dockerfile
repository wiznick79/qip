# syntax=docker/dockerfile:1.7

FROM node:22.14.0-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21-alpine AS application-build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src/ src/
COPY config/ config/
COPY --from=frontend-build /workspace/frontend/dist frontend/dist/
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S qip && adduser -S -G qip qip \
    && mkdir -p /app/data/documents \
    && chown -R qip:qip /app
WORKDIR /app
COPY --from=application-build --chown=qip:qip /workspace/target/qip-*.jar app.jar
USER qip
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
