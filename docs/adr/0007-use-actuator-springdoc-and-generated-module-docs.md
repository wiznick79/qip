# ADR 0007: Use Actuator, springdoc-openapi, and generated module documentation

- Status: Accepted
- Date: 2026-08-20

## Context

The local MVP has stable REST endpoints and verified Spring Modulith boundaries, but operators and reviewers cannot discover the API, probe application availability, or inspect an architecture diagram without reading source code. Hand-maintained endpoint specifications and diagrams would drift from the implementation.

## Decision

Add Spring Boot Actuator and expose only the health endpoint, with liveness/readiness probes enabled and component details hidden. Add the Spring Boot 4-compatible `springdoc-openapi` WebMVC starter to generate OpenAPI JSON and Swagger UI from the running application. Add Spring Modulith's documentation artifact in test scope and generate PlantUML diagrams plus module canvases from the same `ApplicationModules` model used for boundary verification.

Keep generated Modulith files under Maven's `target` directory rather than committing generated output. CI proves generation succeeds. OpenAPI describes the public HTTP surface but does not weaken runtime validation or make generated AI answers authoritative.

## Alternatives considered

### Hand-written OpenAPI and diagrams

This gives precise editorial control but creates two specifications that can silently diverge from controllers and module boundaries. It can be reconsidered if a contract-first external API becomes necessary.

### Custom health controller

This avoids Actuator but duplicates established availability behavior and would require designing another operational contract. Actuator is already aligned with Spring Boot lifecycle and database health infrastructure.

### Expose every Actuator endpoint

Rejected because environment, configuration, mappings, and metrics may disclose unnecessary operational information. The MVP exposes only health with no details.

## Consequences

- Reviewers can explore the API locally at `/swagger-ui.html` and retrieve `/v3/api-docs`.
- Operators get `/actuator/health` plus liveness/readiness groups without sensitive component details.
- Module diagrams and canvases are reproducible build artifacts rather than manually synchronized images.
- `springdoc-openapi` is a runtime dependency and must remain aligned with the Spring Boot major version.

## Reevaluation triggers

Adopt a contract-first checked-in OpenAPI file when external consumers require explicit compatibility governance. Secure or isolate management endpoints before exposing more than health. Commit rendered diagrams only when a documentation publishing pipeline needs stable artifacts.
