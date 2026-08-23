# syntax=docker/dockerfile:1.7

ARG QIP_VERSION=0.1.0-SNAPSHOT

FROM node:24.19.0-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.15-eclipse-temurin-26-alpine AS application-build
ARG QIP_VERSION
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -Drevision="${QIP_VERSION}" -DskipTests dependency:go-offline
COPY src/ src/
COPY config/ config/
COPY --from=frontend-build /workspace/frontend/dist frontend/dist/
RUN mvn -B -Drevision="${QIP_VERSION}" -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
ARG QIP_VERSION
ARG QIP_REVISION=unknown
LABEL org.opencontainers.image.title="Quality Investigation Platform" \
      org.opencontainers.image.description="Evidence-grounded industrial incident investigation workspace" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.source="https://github.com/wiznick79/qip" \
      org.opencontainers.image.version="${QIP_VERSION}" \
      org.opencontainers.image.revision="${QIP_REVISION}"
RUN addgroup -S qip && adduser -S -G qip qip \
    && mkdir -p /app/data/documents \
    && chown -R qip:qip /app
WORKDIR /app
COPY --from=application-build --chown=qip:qip /workspace/target/qip-*.jar app.jar
USER qip
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
