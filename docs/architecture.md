# Architecture and MVP

Status: initial design baseline  
Last updated: 2026-08-20

## 1. Product definition

The Quality Investigation Platform (QIP) helps an industrial investigator assemble a case, find relevant technical knowledge, and produce a source-backed answer to a question about an incident.

QIP is not an autonomous root-cause engine. In the MVP it is a decision-support system: it retrieves relevant passages from documents, asks a language model to synthesize only that material, cites the passages used, and makes uncertainty visible. A person remains responsible for validating conclusions and deciding corrective action.

The application is standalone. It owns its core domain and data and works with synthetic data, manual input, and uploaded public or sample documents. Company-specific systems may later be connected through adapters, but their types and availability must never be prerequisites for the core application.

### Primary MVP user journey

1. Register an asset such as a machine or production line.
2. Record an incident against that asset.
3. Add observations and manually identified evidence to the incident.
4. Upload a PDF or plain-text technical document.
5. Wait for the document to be extracted, split into searchable passages, and indexed.
6. Ask a question in the context of an incident, optionally limiting the document set.
7. Receive an answer with citations to the source document and passage, plus a clear statement when the sources are insufficient.

This journey is narrow enough to finish but demonstrates domain modeling, persistence, modular design, file processing, retrieval, LLM integration, security boundaries, and testability.

### Intended user experience

The final MVP is a web application, not a collection of endpoints and not a generic chatbot. Its primary screen is an investigation workspace that combines structured incident context with a conversational question panel:

- asset and incident dashboards for finding or opening a case;
- incident details, observations, evidence, and timeline;
- a document library with upload and ingestion status;
- an investigation panel for asking questions in incident context;
- grounded answers with clickable citations that open the relevant document passage;
- an explicit action for a user to promote a generated finding into human-confirmed evidence.

The browser calls only QIP's controlled REST API. It never connects directly to PostgreSQL, pgvector, file storage, or a model provider. The backend remains responsible for authorization, validation, retrieval, model context, citation validation, and provenance.

## 2. MVP scope

### Included

- Assets: create, retrieve, and list equipment records.
- Incidents: create, update basic status, retrieve, and search by asset, status, and date.
- Observations: append human-authored notes to an incident.
- Evidence: attach a typed, source-attributed evidence item to an incident.
- Documents: upload PDF or plain text, retain metadata, extract text, and expose ingestion status.
- Knowledge indexing: split extracted text into passages, create embeddings, and store them in PostgreSQL with pgvector.
- Grounded question answering: retrieve relevant passages and generate an answer constrained to those passages.
- Citations: return document ID, title, page or section when available, passage ID, and a short supporting excerpt.
- Failure visibility: distinguish upload, extraction, embedding, retrieval, and model failures.
- Synthetic demo data and non-proprietary sample documents.
- Automated unit, module-boundary, persistence, and API integration tests.
- A web interface covering the primary incident, document, and investigation journey.

### Explicitly out of scope

- Automated root-cause decisions or corrective-action approval.
- Direct SQL, database, or unrestricted API access for the model.
- EQC, EPC, SIMME, SAP, MES, or CMMS integrations.
- Tool calling over structured incident data.
- Kafka, independently deployed services, Kubernetes, and Azure.
- OCR for scanned images, spreadsheets, and broad office-format support.
- Multi-tenancy, enterprise identity integration, and fine-grained plant authorization.
- Autonomous agents, long-running AI plans, or model fine-tuning.
- Production-scale evaluation and observability stacks.
- Native mobile applications, micro-frontends, and independently deployed frontend services.

Deferring these is a scope decision, not a permanent rejection. Each appears in the learning roadmap with an adoption trigger.

## 3. Core domain model

Identifiers are application-owned opaque IDs (prefer UUIDs). Cross-module references use IDs and public module APIs, not another module's persistence entity.

| Concept | Responsibility and important fields |
| --- | --- |
| `Asset` | Equipment being investigated: ID, name, type, external reference (optional), metadata. |
| `Incident` | Reported abnormal condition: ID, title, description, severity, status, occurred-at time, asset ID, created-at time. |
| `Observation` | Human-authored fact or note appended to an incident: ID, incident ID, text, author reference, observed-at time. |
| `EvidenceItem` | A source-attributed item considered during investigation: ID, incident ID, type, summary, source reference, event time, provenance. It is not automatically a proven cause. |
| `Investigation` | The analysis case for an incident: ID, incident ID, status, question/answer history. The MVP may enforce one open investigation per incident without assuming that is permanent. |
| `SourceDocument` | Uploaded knowledge source: ID, title, media type, checksum, storage reference, ingestion status, failure reason, metadata. |
| `KnowledgePassage` | Extracted and indexed segment: ID, document ID, text, page/section locator, sequence, embedding. This is an internal knowledge concept, not exposed as a writable public resource. |
| `Question` | A user's request within an investigation: ID, investigation ID, text, selected document IDs, asked-at time. |
| `GroundedAnswer` | Generated result: answer text, model information, completion status, citations, and diagnostic metadata safe for users. |
| `Citation` | Link from an answer to a passage: document/passage IDs, human-readable locator, excerpt, and relevance metadata. |

### Invariants

- An incident references an existing asset.
- Observations are append-only in the first version; corrections create a new observation or explicit amendment.
- Evidence always records its origin and must distinguish human-entered, imported, retrieved, and model-generated material.
- A document is searchable only after successful extraction and indexing.
- An answer is `GROUNDED` only when every factual claim intended as source-derived is supported by returned citations. If retrieval is insufficient, the answer says so instead of filling gaps.
- Deleting or replacing a document invalidates its passages; stale passages must never remain retrievable.
- Model output is never persisted as human evidence without an explicit user action and provenance label.

## 4. Module boundaries

Use a package-by-module modular monolith, enforced with Spring Modulith verification tests. Each module exposes a small API package; its domain and persistence implementation remain internal.

| Module | Owns | May depend on |
| --- | --- | --- |
| `assets` | Asset lifecycle and lookup | No business module |
| `incidents` | Incidents, observations, evidence, incident search | `assets` public API |
| `knowledge` | Documents, extraction, chunking, embeddings, vector retrieval | No business module; infrastructure adapters for storage, extraction, embedding, and vector search |
| `investigations` | Investigation workflow, questions, answer assembly, citations, grounding policy | `incidents` and `knowledge` public APIs |
| `bootstrap` | Spring application, configuration composition, cross-cutting web/error/security setup | All modules for assembly only |

Do not start with generic `common`, `util`, or `shared` dumping-ground modules. Small technical primitives may live in a narrowly named package only after repeated use proves the need.

The `ai` concern is deliberately not a top-level business module in the MVP. Embedding is an adapter used by `knowledge`; chat completion is an adapter used by `investigations`. This keeps vendor/model APIs outside the domain and prevents “AI” from becoming an unstructured dependency hub.

### Public ports (conceptual)

The business code depends on application-owned interfaces rather than Spring AI, pgvector, a particular model vendor, or a filesystem API:

```java
interface TextExtractor {
    ExtractedDocument extract(StoredDocument document);
}

interface EmbeddingGenerator {
    List<Embedding> embed(List<String> texts);
}

interface KnowledgeRetriever {
    List<RetrievedPassage> search(KnowledgeQuery query);
}

interface AnswerGenerator {
    GeneratedAnswer answer(AnswerRequest request);
}
```

These are illustrative names, not an instruction to create abstractions before a first implementation needs them. Keep a port when it protects a meaningful boundary or enables deterministic testing.

## 5. Initial architecture

```text
Web client / API client
        |
        v
Spring Boot REST application (single deployable)
  +-------------------------------------------------------+
  | assets | incidents | investigations | knowledge       |
  |                   module APIs/events                  |
  +-------------------------+-----------------------------+
                            |
          +-----------------+------------------+
          |                                    |
          v                                    v
 PostgreSQL + pgvector                 document file storage
 domain data, metadata,                local volume in MVP
 passages, embeddings
          ^
          |
  Spring AI adapters -----------------> configured model provider
  embedding + chat                     outbound HTTPS only
```

There is one application process and one PostgreSQL instance. Module boundaries are code boundaries, not network boundaries. Transactions remain local where practical.

### Frontend delivery

Backend vertical slices come first so that the UI consumes stable, tested contracts. Once assets, incidents, and document upload form a meaningful workflow, add a thin TypeScript web client under `frontend/` in the same repository. Select its framework in a short ADR at that point rather than preselecting one before its requirements exist.

The milestone 6 spike selected React 19 with strict TypeScript and Vite 8, recorded in ADR 0003. The frontend shares the application's release lifecycle and its production assets are packaged into the Spring Boot JAR. Vite proxies `/api` during local development. Independent deployment remains justified only if release cadence, caching, team ownership, or hosting requirements later differ.

The first UI increment covers navigation, assets, incidents, document upload, and ingestion status. The grounded question-and-answer milestone then adds the investigation workspace, source panel, and document passage navigation. The conversational panel is part of a structured case, not a site-wide unconstrained chat box.

### Incident lifecycle and search

Incidents begin in `REPORTED` state. The normal lifecycle is `REPORTED` to `INVESTIGATING` to `RESOLVED` to `CLOSED`; a resolved incident may return to `INVESTIGATING` when new information requires the case to be reopened. Repeating the current status is idempotent, while a closed incident is terminal. These rules belong to the incident domain rather than the controller or persistence adapter.

Incident search accepts optional asset, status, and occurred-at bounds. `from` is inclusive and `to` is exclusive, which makes adjacent time windows unambiguous. Results are ordered by occurred-at time descending and then opaque incident ID ascending, with bounded pagination.

### Incident observations

An observation is a human-authored, append-only statement attached to one incident. It records the supplied observation time, an explicit author reference, and the application recording time. Until authentication is introduced, the author reference is a caller-supplied provenance label rather than a verified identity. The API offers append and paginated timeline retrieval, but no update or delete operation. A correction is therefore another attributable observation rather than a silent rewrite of investigation history.

Observation time cannot be later than the application recording time. Timeline retrieval is ordered by observation time and then opaque observation ID, both ascending, so pagination remains deterministic.

### Incident evidence

An evidence item is a typed, source-attributed investigation input, not a proven cause. The manual API accepts a summary, evidence type, source reference, event time, and submitter reference, then assigns `HUMAN_ENTERED` provenance on the server. Clients cannot select or upgrade provenance; attempts to submit a provenance value are rejected. Until authentication exists, the submitter reference is a caller-supplied provenance label rather than a verified identity.

Evidence is append-only in this increment and is returned through a bounded timeline ordered by event time and then opaque evidence ID. Event time cannot be later than the application recording time. Imported, retrieved, and model-generated provenance values are reserved for future controlled workflows; model output cannot enter human evidence through this manual endpoint.

### Document ingestion flow

1. The API validates size and allowed media type, calculates a checksum, stores the file, and creates a `SourceDocument` in `UPLOADED` state.
2. The knowledge application service extracts text and records `EXTRACTED` or a specific failure state.
3. It creates passages that preserve page/section and sequence metadata.
4. It generates embeddings in batches and writes passages plus vectors transactionally or with idempotent retry semantics.
5. The document becomes `INDEXED` only when all expected passages are searchable.

This may initially run after the upload request using an in-process task executor, with status polling. The persisted ingestion state and an idempotent retry operation must allow interrupted jobs to be recovered after a process restart. It must not hold a database transaction open during file extraction or remote model calls. Kafka is unnecessary at MVP volume; durable messaging becomes useful only when ingestion needs independent scaling, replay, or stronger failure isolation.

The milestone 7 implementation performs extraction and indexing synchronously after upload while preserving persisted state transitions: `UPLOADED` → `EXTRACTING` → `EXTRACTED` → `INDEXING` → `INDEXED`, with `EXTRACTION_FAILED` and `INDEXING_FAILED` as retryable failure states. Repository operations use short transactions; local storage reads, PDF processing, and embedding calls occur between them. Retry endpoints resume failed extraction or indexing, and re-uploading duplicate content resumes incomplete ingestion.

Passages never cross page boundaries. Text is whitespace-normalized and split into at most 800 characters with 120 characters of word-aligned overlap. Page number, document-wide sequence, text digest, embedding model, vector dimensions, and indexing time are retained. All embeddings are produced and validated before a single transaction replaces that document's passages, preventing partial batches or stale vectors from becoming searchable. Retrieval uses bounded exact cosine search over `INDEXED` documents with matching model and dimensions; optional document filters constrain the candidate set. ADR 0004 records why approximate indexes and a fixed vector dimension are deferred.

File identity is the SHA-256 checksum of its bytes. Re-uploading identical content returns the original document, including its original title, rather than creating another metadata record or stored file. The original filename is untrusted display metadata: path components are removed and the local storage key is generated from the opaque document ID. Storage defaults outside the web root and is configurable for deployment.

Only PDF and strict UTF-8 plain text are accepted. Direct PDFBox extraction retains non-empty pages and their one-based page numbers, bounded by configured upload, page, and extracted-character limits. Plain text is one logical page. Encrypted, malformed, scanned-only, or otherwise textless PDFs become `EXTRACTION_FAILED`; OCR is not attempted. ADR 0002 records the Tika/PDFBox comparison and selection.

### Grounded answering flow

1. Validate the investigation and permitted document scope.
2. Convert the question into an embedding.
3. Perform similarity search over eligible passages, applying document filters before or alongside ranking.
4. Build a bounded prompt containing instructions, the question, and numbered passages with provenance.
5. Ask the chat model to answer only from supplied passages and to report insufficient evidence when appropriate.
6. Resolve cited passage markers to application-owned `Citation` objects. Reject or downgrade malformed/unknown citations.
7. Return the answer and citations; retain safe metadata needed for later evaluation.

### AI concepts used in the MVP

- **Embedding:** a numeric representation of text in which semantically similar passages tend to be near one another. It solves vocabulary mismatch better than exact keyword search. It does not understand truth and is not an answer by itself. An initial alternative is PostgreSQL full-text search; hybrid keyword/vector retrieval can be evaluated later.
- **Vector store:** storage plus an index for comparing embeddings. pgvector keeps this capability beside existing PostgreSQL data, avoiding another service in the MVP. A dedicated vector database is justified only by measured scale or operational needs.
- **Semantic search:** retrieving by meaning similarity using embeddings. It finds candidate evidence, but similarity is not proof of relevance.
- **RAG (retrieval-augmented generation):** retrieve passages first, then give only those passages to the model as context for an answer. It improves grounding and allows citations, but does not eliminate hallucinations.
- **Grounding:** tying an answer to supplied evidence. The platform enforces it through constrained prompts, explicit passage identifiers, citation validation, and an “insufficient evidence” path.
- **Tokens:** the model's units of input and output. Retrieved context, instructions, and answers all consume tokens, affecting cost, latency, and context limits. Passage count and size therefore need hard bounds.
- **Prompt injection:** malicious or accidental instructions inside uploaded content, such as “ignore previous rules.” Documents are untrusted data, never instructions. The prompt separates them clearly, tools are absent from MVP RAG, and output/citations are validated. Stronger defenses and adversarial tests are a later security milestone.

## 6. Technology decisions

The bootstrap pins Spring Boot 4.1.0, Spring Modulith 2.1.0, and Spring AI 2.0.0, compatible stable release lines selected in August 2026. Only Spring AI's provider-neutral model API is included; provider starters remain an explicit deployment choice.

### MVP technologies

| Technology | Why it belongs now |
| --- | --- |
| Java 21+ | Modern supported Java baseline and familiar core language. |
| Spring Boot | Web, configuration, validation, persistence, and production conventions. |
| Spring Modulith | Verifies module boundaries and supports an explainable path from modules to services. |
| Maven Wrapper | Reproducible local and CI builds without requiring a global Maven version. |
| PostgreSQL 17 | Durable relational store for the owned domain. |
| pgvector 0.8.6 | Semantic retrieval without operating a separate database. The versioned PostgreSQL 17 image is shared by Compose and integration tests. |
| Flyway | Reviewable, repeatable database schema evolution. |
| Spring Data JPA | Straightforward transactional domain persistence; vector queries may use JDBC/native SQL behind the knowledge repository. |
| Spring AI | Consistent Java/Spring adapters for embedding and chat models while application ports remain provider-neutral. |
| Apache PDFBox 3.0.8 adapter | Bounded, page-aware PDF extraction for the deliberately small PDF/plain-text format set; ADR 0002 records the spike. |
| Bean Validation and RFC 9457 problem details | Stable API input and error behavior. |
| Docker Compose | Reproducible PostgreSQL/pgvector development dependency; the application can run from the IDE. |
| JUnit 5, AssertJ, Mockito, Testcontainers | Fast domain tests and realistic PostgreSQL/pgvector integration tests. |
| Spring Boot Actuator + structured logging | Basic health and diagnostics now, without deploying an observability platform. |
| OpenAPI generation/documentation | Makes the portfolio API discoverable and testable. |

Model credentials must come from environment variables or a local ignored configuration file. The design must allow a deterministic fake embedding/chat adapter so most tests and normal domain development need no network or paid model.

### Later learning milestones and adoption triggers

| Milestone | Introduce when | Learning outcome |
| --- | --- | --- |
| Structured tool calling | Document-only Q&A works and incident queries have stable use cases. | Constrained functions, schemas, authorization, result minimization, audit trail. |
| Provider connector layer | A second real or mock source must supply generic evidence. | Ports/adapters, anti-corruption mapping, timeouts, retries, partial availability. |
| AI evaluation | A representative synthetic question/evidence set exists. | Retrieval metrics, groundedness/citation checks, regression gates, model comparison. |
| AI security hardening | Before accepting untrusted/public uploads or enabling tools. | Prompt-injection tests, content isolation, tool authorization, data leakage controls. |
| OpenTelemetry + Prometheus/Grafana | Multiple slow/remote stages make logs inadequate. | Traces across retrieval/model calls, RED metrics, token/cost/latency dashboards. |
| Kafka | Ingestion needs durable buffering, replay, high throughput, or independent failure handling. | Event schemas, idempotent consumers, retries, dead-letter handling, eventual consistency. |
| Extract ingestion worker | Profiling shows extraction/embedding has a different scaling or release lifecycle. | First evidence-based service extraction and distributed tracing. |
| CI/CD | Begin once the initial build and tests are stable. | GitHub Actions, dependency/cache strategy, image build, quality gates. |
| Azure deployment | A local MVP is demonstrable and secrets/storage/database needs are understood. | Managed database, object storage, identity, networking, cost controls. |
| Kubernetes | At least two independently scalable services exist and orchestration benefits exceed operational cost. | Deployments, services, probes, configuration, autoscaling, rollout behavior. |

## 7. Repository and package structure

Start with one Maven module. A multi-module build does not create stronger domain boundaries by itself and adds friction before extraction is needed.

```text
qip/
  AGENTS.md
  README.md
  pom.xml
  mvnw, mvnw.cmd, .mvn/
  compose.yaml
  docs/
    architecture.md
    adr/
  src/
    main/
      java/io/github/wiznick79/qip/
        QipApplication.java
        assets/
          api/
          internal/
        incidents/
          api/
          internal/
        knowledge/
          api/
          internal/
        investigations/
          api/
          internal/
        bootstrap/
      resources/
        application.yml
        db/migration/
    test/
      java/io/github/wiznick79/qip/
  samples/
    documents/
    data/
```

The bootstrap uses `io.github.wiznick79.qip` as its public package namespace. Inside each `internal` package, add layers only when useful, commonly `domain`, `application`, and `infrastructure`. REST controllers may live in the module's API/infrastructure edge but must call application use cases rather than repositories directly.

Spring Modulith modules are rooted at `assets`, `incidents`, `knowledge`, and `investigations`. Prefer package-private implementation types. Expose module APIs deliberately (for example, with an explicitly named `api` interface package); avoid reaching into another module's `internal` packages.

## 8. API sketch

This is a planning contract, not a frozen specification:

```text
POST   /api/assets
GET    /api/assets/{assetId}
GET    /api/assets

POST   /api/incidents
GET    /api/incidents/{incidentId}
GET    /api/incidents?assetId=&status=&from=&to=
PATCH  /api/incidents/{incidentId}/status
POST   /api/incidents/{incidentId}/observations
GET    /api/incidents/{incidentId}/observations
POST   /api/incidents/{incidentId}/evidence
GET    /api/incidents/{incidentId}/evidence

POST   /api/documents                 multipart upload
GET    /api/documents/{documentId}
GET    /api/documents/{documentId}/status
POST   /api/documents/{documentId}/extraction
POST   /api/documents/{documentId}/indexing
GET    /api/documents?page=&size=

POST   /api/incidents/{incidentId}/investigations
POST   /api/investigations/{investigationId}/questions
GET    /api/investigations/{investigationId}
```

The question response includes answer status, answer text, citations, and model/retrieval metadata suitable for debugging without exposing secrets or hidden prompts.

## 9. Quality, security, and operational baseline

- Validate request shape, ranges, dates, IDs, upload size, and media type at the boundary.
- Treat filenames, document text, metadata, and model output as untrusted input.
- Store uploaded files outside the web root using generated storage keys; never construct paths from user filenames.
- Do not log document bodies, prompts, credentials, embeddings, or full model responses by default.
- Configure timeouts and bounded retries for model calls. Do not retry validation, authorization, or deterministic extraction errors.
- Bound passage size, retrieval count, prompt size, answer size, and concurrent ingestion work.
- Return correlation IDs and machine-readable problem details.
- Use Flyway exclusively for schema changes; production-like profiles must not rely on JPA auto-DDL.
- Record model/provider identifiers and token usage when available, but keep vendor response types outside domain APIs.
- Keep secrets out of Git. Include only `.env.example`-style names with empty/example values.
- Use synthetic names and values in fixtures, screenshots, commits, and documentation.

## 10. First implementation tasks, in dependency order

Each numbered item should be a small, independently reviewable change. Do not combine the full sequence into one pull request.

1. **Bootstrap and boundary proof**
   - Create Java/Spring Boot Maven project and wrapper.
   - Add Spring Modulith and a module verification test with empty module roots.
   - Add formatting/static-analysis baseline, test command, and a minimal CI workflow.
   - Add an ADR recording modular monolith and single Maven module decisions.

2. **Local persistence foundation**
   - Add PostgreSQL/pgvector Compose service, Flyway, Testcontainers, profiles, and secret-safe example configuration.
   - Prove migration and container startup with one integration test; do not create all domain tables speculatively.

3. **Asset vertical slice**
   - Implement asset domain, migration, repository adapter, use cases, REST endpoints, validation, problem responses, and tests.
   - Establish API DTO and entity separation conventions.

4. **Incident vertical slice**
   - Implement incident lifecycle and search, referencing assets through the assets module API.
   - Add observations, then evidence as separate reviewable changes.
   - Verify module dependencies and transaction boundaries.

5. **Document storage and extraction spike**
   - Implement safe upload, metadata, local storage adapter, checksum/idempotency policy, status state machine, and PDF/plain-text extraction.
   - Compare Tika/PDFBox on the sample documents and record the choice in an ADR.
   - No embeddings or LLM calls yet.

6. **Frontend foundation**
   - Record the TypeScript framework/build choice in an ADR after a small spike.
   - Add a thin web client for assets, incidents, document upload, and ingestion status.
   - Keep it in this repository and release lifecycle; consume only documented REST APIs.

7. **Knowledge indexing**
   - Define passage metadata and chunking policy.
   - Add embedding port with deterministic fake plus Spring AI adapter.
   - Add pgvector migration, batch indexing, idempotent reprocessing, and similarity-search tests.

8. **Grounded question-answering vertical slice**
   - Implement investigation/question model, retrieval orchestration, bounded prompt construction, answer adapter, citation parsing/validation, and insufficient-evidence behavior.
   - Add fake-model contract tests and a small opt-in live-model smoke test.
   - Extend the web client with the investigation workspace, grounded-answer statuses, citations, and passage navigation.

9. **Demo and MVP hardening**
   - Add synthetic seed data, public/sample documents, scripted demo path, OpenAPI docs, upload/security edge-case tests, health endpoints, and architecture diagrams generated from verified modules.
   - Define a small evaluation set before declaring the MVP complete.

## 11. Architectural decision policy

Record an ADR for choices that are costly to reverse, cross module boundaries, add an external service, or materially change security/operational behavior. Each ADR states context, decision, alternatives, consequences, and a reevaluation trigger.

A technology enters the project only when the current problem can be stated first, its simpler alternatives have been considered, and success can be measured. Portfolio value comes from being able to explain these decisions and trade-offs, not from maximizing the technology count.
