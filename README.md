# Quality Investigation Platform (QIP)

QIP is a standalone, AI-assisted platform for investigating industrial incidents and quality problems. It combines user-entered incident data with uploaded technical knowledge, then helps investigators find and summarize relevant evidence while preserving the source of every result.

The project is intentionally starting as a modular Java application. Kafka, microservices, Kubernetes, and cloud deployment are later learning milestones that must be justified by an actual architectural need.

Start with:

- [Architecture and MVP](docs/architecture.md)
- [Engineering instructions](AGENTS.md)
- [ADR 0001: modular monolith and single Maven module](docs/adr/0001-modular-monolith-and-single-maven-module.md)
- [ADR 0002: bounded PDF extraction with PDFBox](docs/adr/0002-use-pdfbox-for-bounded-pdf-extraction.md)
- [ADR 0003: React and Vite web client](docs/adr/0003-use-react-and-vite-for-the-web-client.md)

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
```

Uploads are limited to 10 MiB and to `application/pdf` or UTF-8 `text/plain`. QIP stores files outside the web root under generated keys, computes a SHA-256 checksum, returns the existing document for duplicate content, and extracts text synchronously without holding a database transaction open during file processing. PDF page numbers are retained for later citations. Malformed, encrypted, scanned-only, or extraction-limit-breaking PDFs remain visible as `EXTRACTION_FAILED` and may be retried.

The storage directory defaults to `./data/documents` and can be overridden with `QIP_DOCUMENT_STORAGE_DIRECTORY`. Uploaded content and extracted text are intentionally ignored by Git.

Document metadata can be listed with bounded pagination:

```text
GET /api/documents?page=0&size=20
```

## Web client

The React and TypeScript client lives under `frontend/`. Start Spring Boot on port 8080, then run:

```shell
cd frontend
npm ci
npm run dev
```

Vite serves the development UI at `http://localhost:5173` and proxies `/api` to Spring Boot. Run `npm run verify` for type-checking, behavior tests, and the production build. Maven packages an existing `frontend/dist` into the application JAR, and CI always builds the frontend before Maven verification.

The first UI increment covers asset registration, incident reporting/filtering, document upload, and ingestion status. It deliberately has no generic chat panel; the structured investigation workspace arrives with grounded question answering.

The repository currently contains the milestone 6 frontend foundation. Embeddings and LLM integration have not been implemented yet.
