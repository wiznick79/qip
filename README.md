# Quality Investigation Platform (QIP)

QIP is a standalone, AI-assisted platform for investigating industrial incidents and quality problems. It combines user-entered incident data with uploaded technical knowledge, then helps investigators find and summarize relevant evidence while preserving the source of every result.

The project is intentionally starting as a modular Java application. Kafka, microservices, Kubernetes, and cloud deployment are later learning milestones that must be justified by an actual architectural need.

Start with:

- [Architecture and MVP](docs/architecture.md)
- [Engineering instructions](AGENTS.md)
- [ADR 0001: modular monolith and single Maven module](docs/adr/0001-modular-monolith-and-single-maven-module.md)
- [ADR 0002: bounded PDF extraction with PDFBox](docs/adr/0002-use-pdfbox-for-bounded-pdf-extraction.md)
- [ADR 0003: React and Vite web client](docs/adr/0003-use-react-and-vite-for-the-web-client.md)
- [ADR 0004: pgvector and embedding ports](docs/adr/0004-use-pgvector-with-application-owned-embedding-ports.md)
- [ADR 0005: grounded answers and citation validation](docs/adr/0005-grounded-answer-orchestration-and-citation-validation.md)
- [ADR 0006: local Ollama model provider](docs/adr/0006-use-ollama-for-local-model-inference.md)
- [ADR 0007: operational API and generated module documentation](docs/adr/0007-use-actuator-springdoc-and-generated-module-docs.md)
- [ADR 0008: explicit human review for findings](docs/adr/0008-require-explicit-human-review-for-findings.md)
- [ADR 0009: investigation closure after human review](docs/adr/0009-close-investigations-only-after-human-review.md)
- [Local MVP demonstration](docs/demo.md)

## Development

Prerequisites: Java 21 or newer. Use the committed Maven Wrapper for all project commands.

```shell
./mvnw verify
```

On Windows PowerShell:

```powershell
.\mvnw.cmd verify
```

Apply the Java formatter with `./mvnw spotless:apply`. The `verify` lifecycle compiles the application, runs tests, verifies Spring Modulith boundaries, checks formatting, and runs the static-analysis baseline.

The integration test suite uses Testcontainers and therefore requires a running Docker engine.

## Local database

The local dependency is PostgreSQL 17 with pgvector 0.8.6. Start it with:

```shell
docker compose up -d --wait
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

On Windows PowerShell, run the application with:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The `local` profile uses deliberately non-secret development defaults from `application-local.yml`. Copy `.env.example` to `.env` only when you need to override them. Other environments should provide standard Spring datasource configuration through secret management rather than activate the `local` profile.

Stop the database with `docker compose down`. Its named volume is preserved; add `--volumes` only when you intentionally want to delete local database data.

## Current API

The asset vertical slice provides:

```text
POST /api/assets
GET  /api/assets/{assetId}
GET  /api/assets?page=0&size=20
```

Asset lists are sorted by name and ID and bounded to 100 records per page. Invalid requests and missing assets return RFC 9457 problem details.

The incident lifecycle slice additionally provides:

```text
POST  /api/incidents
GET   /api/incidents/{incidentId}
GET   /api/incidents?assetId=&status=&from=&to=&page=0&size=20
PATCH /api/incidents/{incidentId}/status
POST  /api/incidents/{incidentId}/observations
GET   /api/incidents/{incidentId}/observations?page=0&size=20
POST  /api/incidents/{incidentId}/evidence
GET   /api/incidents/{incidentId}/evidence?page=0&size=20
```

Incident search is ordered newest first and supports bounded pagination. Incidents follow the documented `REPORTED` → `INVESTIGATING` → `RESOLVED` → `CLOSED` lifecycle, with reopening from `RESOLVED` to `INVESTIGATING`.

Observations are append-only, explicitly attributed, and returned in deterministic timeline order. Silent update and delete operations are deliberately absent.

Evidence is typed, source-attributed, append-only, and explicitly returned as `HUMAN_ENTERED` provenance. It remains investigation input rather than a confirmed cause.

The document ingestion slice additionally provides:

```text
POST /api/documents                         multipart fields: title, file
GET  /api/documents/{documentId}
GET  /api/documents/{documentId}/status
POST /api/documents/{documentId}/extraction retry failed extraction
POST /api/documents/{documentId}/indexing   retry or deliberately re-index
```

Uploads are limited to 10 MiB and to `application/pdf` or UTF-8 `text/plain`. QIP stores files outside the web root under generated keys, computes a SHA-256 checksum, returns the existing document for duplicate content, and extracts and indexes text synchronously without holding a database transaction open during file processing or embedding. PDF page numbers are retained for later citations. Malformed, encrypted, scanned-only, or extraction-limit-breaking PDFs remain visible as `EXTRACTION_FAILED`; embedding failures remain visible as `INDEXING_FAILED`. Both may be retried.

Extracted pages are split into bounded, overlapping passages and stored with pgvector embeddings. The default `deterministic-hash-v1` adapter is offline and credential-free for development and tests. It offers reproducible lexical similarity, not production semantic quality. The `ollama` profile replaces it with the locally configured Spring AI Ollama embedding model. Calling the indexing endpoint for an `INDEXED` document deliberately rebuilds and atomically replaces its passages, which is required after changing embedding models.

The storage directory defaults to `./data/documents` and can be overridden with `QIP_DOCUMENT_STORAGE_DIRECTORY`. Uploaded content and extracted text are intentionally ignored by Git.

Document metadata can be listed with bounded pagination:

```text
GET /api/documents?page=0&size=20
```

The grounded investigation slice additionally provides:

```text
POST /api/incidents/{incidentId}/investigations
GET  /api/investigations/{investigationId}
POST /api/investigations/{investigationId}/questions
POST /api/investigations/{investigationId}/findings
POST /api/investigations/{investigationId}/findings/{findingId}/reviews
POST /api/investigations/{investigationId}/closure
```

Creating an investigation is idempotent per incident. Questions may optionally select document IDs and return `GROUNDED`, `INSUFFICIENT_EVIDENCE`, or `TECHNICAL_FAILURE`. Grounded responses include validated citation snapshots with document, page, passage, excerpt, and relevance metadata. The default answer adapter is deterministic and offline. The opt-in `ollama` profile supplies local `EmbeddingModel` and `ChatModel` beans without API keys.

A grounded answer with validated citations can be explicitly proposed as a `DRAFT` finding. A separate review action records `CONFIRMED` or `REJECTED`, the reviewer reference, a mandatory rationale, and an append-only audit event. Insufficient or failed answers cannot become findings, and reviewed findings cannot be overwritten. Actor references remain caller-supplied provenance labels until authentication is introduced.

An investigation can be closed only after every draft is resolved and at least one finding is confirmed. Closure records an immutable human-authored summary, closer reference, and application timestamp. A closed investigation rejects new questions, finding actions, and repeated closure.

## Local Ollama models

Start Ollama with the required models already installed, then run QIP with both the database and model profiles:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,ollama"
```

The defaults match the initial local development setup:

```text
QIP_OLLAMA_BASE_URL=http://localhost:11434
QIP_OLLAMA_CHAT_MODEL=qwen3-coder:30b
QIP_OLLAMA_EMBEDDING_MODEL=nomic-embed-text:latest
```

QIP never pulls models automatically. Override those environment variables to select other installed tags. Model output remains untrusted: malformed responses and unknown citation identifiers are rejected by application-owned validation.

Documents previously indexed by the deterministic adapter must be re-indexed once under the Ollama profile. For the synthetic dataset, run:

```powershell
.\scripts\load-synthetic-demo.ps1 -ReindexDocuments
```

Run the opt-in local model smoke test with:

```powershell
$env:QIP_LIVE_MODEL_TEST = "true"
.\mvnw.cmd "-Dtest=SpringAiAnswerGeneratorLiveTests" test
```

## Web client

The React and TypeScript client lives under `frontend/`. Start Spring Boot on port 8080, then run:

```shell
cd frontend
npm ci
npm run dev
```

Vite serves the development UI at `http://localhost:5173` and proxies `/api` to Spring Boot. Run `npm run verify` for type-checking, behavior tests, and the production build. Maven packages an existing `frontend/dist` into the application JAR, and CI always builds the frontend before Maven verification.

The web client covers asset registration, paginated incident reporting and lifecycle actions, document upload/status, and a structured investigation workspace. Incident rows link directly to bookmarkable case URLs. The Investigate screen scopes questions to an incident, optionally filters indexed documents, distinguishes grounded and insufficient answers, exposes citation passages, and provides explicit finding proposal and review controls. Investigation closure and incident resolution remain separate human actions. It is not a site-wide unconstrained chat box.

The repository contains the local MVP implemented through Milestone 12, including explicit finding review, terminal investigation closure, paginated case navigation, and deliberate incident lifecycle actions. Deterministic fake embedding and answer models remain the default, while the explicit `ollama` profile enables local semantic retrieval and grounded answer generation without credentials.

API and operational endpoints are available while QIP is running:

```text
Swagger UI  http://localhost:8080/swagger-ui.html
OpenAPI     http://localhost:8080/v3/api-docs
Health      http://localhost:8080/actuator/health
Liveness    http://localhost:8080/actuator/health/liveness
Readiness   http://localhost:8080/actuator/health/readiness
```

Only health is exposed through Actuator, and component details are hidden. Maven verification also generates Spring Modulith PlantUML diagrams and module canvases under `target/spring-modulith-docs/`.

## Synthetic demo data

The repository includes three fictional machines and matching three-page PDF manuals for local testing. With QIP running on port 8080, load them through the public API using PowerShell 7:

```powershell
.\scripts\load-synthetic-demo.ps1
```

Pass `-BaseUrl` when the application uses another address. The loader skips assets that already have the same synthetic external reference, while document uploads reuse existing content through QIP's checksum policy. The source manifest is `samples/machines.json`; generated documents live under `output/pdf/`. Every name, value, scenario, and procedure is invented and must not be applied to real equipment.

Run the complete scripted case with:

```powershell
.\scripts\run-synthetic-investigation.ps1 -ReindexDocuments
```

The versioned retrieval regression set is under `samples/evaluation/`. The default build evaluates it with the deterministic offline embedding adapter; see [the demo walkthrough](docs/demo.md) for the full local flow.
