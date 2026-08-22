# ADR 0010: Package the web client and backend in one container

## Status

Accepted

## Context

QIP's React client and Spring Boot backend share one release lifecycle, and Maven already packages the production frontend bundle into the application JAR. Local operation previously required separate commands for PostgreSQL, Spring Boot, and optionally Vite. Running an independent production frontend container would add a web server, proxy configuration, cross-origin concerns, and a second deployable without a demonstrated ownership or scaling need.

## Decision

Build one QIP application image with a multi-stage Dockerfile. A pinned Node build stage verifies and produces the frontend bundle, a pinned Maven/Java 21 stage packages that bundle into the Spring Boot JAR, and a non-root Java 21 runtime stage runs the single artifact.

Docker Compose starts the QIP image and the pinned PostgreSQL/pgvector image, waits on explicit health checks, and persists database and uploaded-document data in separate named volumes. Local Ollama remains a host service reached through `host.docker.internal`; QIP never downloads models. This avoids duplicating large model storage and introducing GPU-specific container configuration into the default local workflow.

The Compose credentials are visibly local-only defaults and remain overridable. They are not production secret management.

## Consequences

- The complete web application is available from one port and starts with one Compose command.
- Production-like local startup no longer needs Java, Maven, Node, or npm installed on the host after the image has been built.
- Frontend and backend continue to deploy atomically, matching their current ownership and release cadence.
- A source change requires rebuilding the QIP image; Vite and `spring-boot:run` remain available for rapid development.
- Ollama must already be running on the host when the `ollama` profile is selected.

## Reevaluation triggers

Split the frontend into an independent image only when release cadence, caching, team ownership, edge delivery, or scaling requirements materially differ. Add an Ollama service to Compose only when portable model storage and tested CPU/GPU runtime configuration justify its operational cost.
