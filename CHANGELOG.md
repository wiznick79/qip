# Changelog

QIP follows [Semantic Versioning](https://semver.org/) before and after 1.0. While the project is pre-1.0, minor releases may contain intentional API changes documented here.

## [Unreleased]

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

[Unreleased]: https://github.com/wiznick79/qip/commits/main
