# Changelog

QIP follows [Semantic Versioning](https://semver.org/) before and after 1.0. While the project is pre-1.0, minor releases may contain intentional API changes documented here.

## [Unreleased]

## [0.2.0] - 2026-08-26

### Added

- Authenticated browser sessions with configurable synthetic `INVESTIGATOR`, `REVIEWER`, and `ADMIN` users, role-protected actions, and principal-derived attribution.
- Dashboard landing page, fixed application navigation, expanded frontend security tests, and clearer incident/investigation lifecycle presentation.
- Versioned end-to-end RAG quality gate covering retrieval, bounded prompts, answer status, citation validation, persistence, and adversarial document content.
- Blinded twelve-case Ollama comparison workflow with objective gates, human scoring, latency reporting, and a separate model reveal key.
- Correlation-aware structured logs, bounded Micrometer pipeline metrics, protected Actuator metrics, and an administrator Operations dashboard.
- Portable single-VM hosted deployment topology with Caddy TLS, private application/database networking, persistent volumes, backups, readiness checks, and automatic rollback.
- Tag-driven hosted deployment workflow for attested semantic-release images.

### Changed

- Selected `qwen3.5:9b` as the configurable local Ollama chat default after workload-specific evaluation; thinking remains disabled and the context remains bounded by default.
- Updated the supported build toolchain and pinned container/action dependencies while retaining Java 21 as the runtime baseline.
- Investigation closure now synchronizes the associated incident lifecycle without requiring a redundant manual resolution step.

### Security

- Replaced caller-supplied human identity labels with authenticated principals and enforced reviewer/investigator role boundaries.
- Added stable RFC 9457 authentication and authorization failures without exposing internal details.
- Kept document bodies, prompts, model responses, credentials, and authorization headers out of operational logs and metrics.

## [0.1.0] - 2026-08-23

### Added

- Modular Spring Boot backend with verified `assets`, `incidents`, `knowledge`, and `investigations` boundaries.
- React investigation workspace for assets, incidents, observations, human-entered evidence, documents, grounded questions, reviewed findings, and case closure.
- Bounded PDF and text ingestion with provenance-preserving passages, pgvector retrieval, deterministic offline adapters, and opt-in local Ollama inference.
- Synthetic three-machine demonstration, retrieval evaluation set, OpenAPI documentation, health probes, and generated Spring Modulith documentation.
- Single non-root application image containing the production frontend and backend, with a PostgreSQL/pgvector Compose stack.
- Tag-driven GitHub releases with a versioned JAR, SHA-256 checksum, GHCR image, and build-provenance attestations.
- Apache License 2.0 with Maven and OCI license metadata.

### Security

- Citation identifiers are validated against the exact retrieved context; untrusted documents cannot supply model instructions or tools.
- Human confirmation and rationale are required before a generated answer becomes a confirmed finding.

[Unreleased]: https://github.com/wiznick79/qip/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/wiznick79/qip/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/wiznick79/qip/releases/tag/v0.1.0
