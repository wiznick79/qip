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
POST /api/documents/{documentId}/indexing   retry failed indexing
```

Uploads are limited to 10 MiB and to `application/pdf` or UTF-8 `text/plain`. QIP stores files outside the web root under generated keys, computes a SHA-256 checksum, returns the existing document for duplicate content, and extracts and indexes text synchronously without holding a database transaction open during file processing or embedding. PDF page numbers are retained for later citations. Malformed, encrypted, scanned-only, or extraction-limit-breaking PDFs remain visible as `EXTRACTION_FAILED`; embedding failures remain visible as `INDEXING_FAILED`. Both may be retried.

Extracted pages are split into bounded, overlapping passages and stored with pgvector embeddings. The default `deterministic-hash-v1` adapter is offline and credential-free for development and tests. It offers reproducible lexical similarity, not production semantic quality. A Spring AI 2.0 adapter is available under the `spring-ai` profile, but a provider implementation and `EmbeddingModel` configuration must be added deliberately before activating it.

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
```

Creating an investigation is idempotent per incident. Questions may optionally select document IDs and return `GROUNDED`, `INSUFFICIENT_EVIDENCE`, or `TECHNICAL_FAILURE`. Grounded responses include validated citation snapshots with document, page, passage, excerpt, and relevance metadata. The default answer adapter is deterministic and offline. The `spring-ai` profile requires explicitly configured `EmbeddingModel` and `ChatModel` provider beans; QIP does not ship provider credentials or select a paid model by default.

## Web client

The React and TypeScript client lives under `frontend/`. Start Spring Boot on port 8080, then run:

```shell
cd frontend
npm ci
npm run dev
```

Vite serves the development UI at `http://localhost:5173` and proxies `/api` to Spring Boot. Run `npm run verify` for type-checking, behavior tests, and the production build. Maven packages an existing `frontend/dist` into the application JAR, and CI always builds the frontend before Maven verification.

The web client covers asset registration, incident reporting/filtering, document upload/status, and a structured investigation workspace. The Investigate screen scopes questions to an incident, optionally filters indexed documents, distinguishes grounded and insufficient answers, and exposes citation passages. It is not a site-wide unconstrained chat box.

The repository currently contains the milestone 8 grounded question-answering slice. A deterministic fake embedding and answer model make the entire workflow usable without credentials; no live provider is configured by default.

## Synthetic demo data

The repository includes three fictional machines and matching three-page PDF manuals for local testing. With QIP running on port 8080, load them through the public API using PowerShell 7:

```powershell
.\scripts\load-synthetic-demo.ps1
```

Pass `-BaseUrl` when the application uses another address. The loader skips assets that already have the same synthetic external reference, while document uploads reuse existing content through QIP's checksum policy. The source manifest is `samples/machines.json`; generated documents live under `output/pdf/`. Every name, value, scenario, and procedure is invented and must not be applied to real equipment.
