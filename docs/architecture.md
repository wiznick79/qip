# Architecture and MVP

Status: v0.2.0 portfolio release; milestones 14–17 complete
Last updated: 2026-08-26

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
| `Investigation` | The analysis case for an incident: ID, incident ID, status, question/answer history, findings, and immutable human-authored closure details. The MVP enforces one investigation per incident without assuming that is permanent. |
| `SourceDocument` | Uploaded knowledge source: ID, title, media type, checksum, storage reference, ingestion status, failure reason, metadata. |
| `KnowledgePassage` | Extracted and indexed segment: ID, document ID, text, page/section locator, sequence, embedding. This is an internal knowledge concept, not exposed as a writable public resource. |
| `Question` | A user's request within an investigation: ID, investigation ID, text, selected document IDs, asked-at time. |
| `GroundedAnswer` | Generated result: answer text, model information, completion status, citations, and diagnostic metadata safe for users. |
| `Citation` | Link from an answer to a passage: document/passage IDs, human-readable locator, excerpt, and relevance metadata. |
| `Finding` | A human-proposed conclusion sourced from one grounded answer. It remains `DRAFT` until explicitly `CONFIRMED` or `REJECTED` with reviewer provenance and rationale. |

### Invariants

- An incident references an existing asset.
- Observations are append-only in the first version; corrections create a new observation or explicit amendment.
- Evidence always records its origin and must distinguish human-entered, imported, retrieved, and model-generated material.
- A document is searchable only after successful extraction and indexing.
- An answer is `GROUNDED` only when every factual claim intended as source-derived is supported by returned citations. If retrieval is insufficient, the answer says so instead of filling gaps.
- Deleting or replacing a document invalidates its passages; stale passages must never remain retrievable.
- Model output is never persisted as human evidence without an explicit user action and provenance label.
- Only a grounded answer with validated citations can source a finding; review is terminal and append-only audit events preserve proposal and decision provenance.
- Closing an investigation requires no unresolved drafts and at least one confirmed finding; closure is terminal and blocks further questions or finding actions.

## 4. Module boundaries

Use a package-by-module modular monolith, enforced with Spring Modulith verification tests. Each module exposes a small API package; its domain and persistence implementation remain internal.

| Module | Owns | May depend on |
| --- | --- | --- |
| `assets` | Asset lifecycle and lookup | No business module |
| `incidents` | Incidents, observations, evidence, incident search | `assets` public API |
| `knowledge` | Documents, extraction, chunking, embeddings, hybrid retrieval | No business module; infrastructure adapters for storage, extraction, embedding, and PostgreSQL search |
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

The local container workflow preserves that single deployable. A multi-stage build produces the React bundle, packages it into the Spring Boot JAR, and copies the artifact into a non-root Java runtime image. Compose runs that QIP image with PostgreSQL/pgvector, health-gates application startup, and persists database and document storage separately. Host Ollama is reached through `host.docker.internal` so the default workflow does not duplicate large model artifacts or assume portable GPU container configuration. ADR 0010 records the decision.

The milestone 17 hosted topology keeps the same deployable on one modest Linux VM. Caddy is the only public service and terminates TLS before forwarding to QIP; QIP and PostgreSQL communicate only on the private Compose network. PostgreSQL data, document files, and certificate state use separate volumes, while application secrets remain in a mode-0600 host file. A GitHub environment gates semantic-release deployments, verifies the image attestation, and invokes readiness-based image rollback. The hosted demonstration deliberately uses deterministic adapters because local Ollama model memory is incompatible with the selected 2 GB cost boundary. ADR 0014 records the target, alternatives, operational costs, and reevaluation triggers.

The first UI increment covers navigation, assets, incidents, document upload, and ingestion status. The grounded question-and-answer milestone then adds the investigation workspace, source panel, and document passage navigation. The conversational panel is part of a structured case, not a site-wide unconstrained chat box.

### Incident lifecycle and search

Incidents begin in `REPORTED` state. The normal lifecycle is `REPORTED` to `INVESTIGATING` to `RESOLVED` to `CLOSED`; a resolved incident may return to `INVESTIGATING` when new information requires the case to be reopened. Repeating the current status is idempotent, while a closed incident is terminal. These rules belong to the incident domain rather than the controller or persistence adapter.

Incident search accepts optional asset, status, and occurred-at bounds. `from` is inclusive and `to` is exclusive, which makes adjacent time windows unambiguous. Results are ordered by occurred-at time descending and then opaque incident ID ascending, with bounded pagination.

The milestone 12 web workflow consumes that pagination in 20-record pages and exposes only valid next lifecycle actions. Starting an investigation moves a reported incident to `INVESTIGATING`. Successfully closing that investigation atomically moves the incident to `RESOLVED`, because the investigation closure is the human decision that completes the active case workflow. Resolved incidents may be reopened or explicitly moved to terminal `CLOSED` later as an archival action.

Primary screens use lightweight hash routes, including an incident identifier for an investigation workspace. This makes case links bookmarkable and restores the selected case after refresh without introducing a routing dependency. The compact investigation picker remains bounded to recent records; the paginated incident queue is the complete browsing surface.

The authenticated web client opens on a lightweight dashboard assembled from existing paginated APIs. It presents workflow totals, the five newest incidents, and direct navigation actions without introducing a dashboard-specific backend or duplicating domain state.

Milestone 13 adds a bookmarkable incident record at the same incident route. It presents incident context, independently paginated observation and evidence timelines, append-only input forms, and a direct investigation action. Milestone 14 derives actor references from the authenticated principal, while evidence provenance remains server-assigned. These records are deliberately not included in model context yet; doing so requires a separate bounded, provenance-aware design and citation policy.

### Incident observations

An observation is a human-authored, append-only statement attached to one incident. It records the supplied observation time, the authenticated principal as author reference, and the application recording time. The API offers append and paginated timeline retrieval, but no update or delete operation. A correction is therefore another attributable observation rather than a silent rewrite of investigation history.

Observation time cannot be later than the application recording time. Timeline retrieval is ordered by observation time and then opaque observation ID, both ascending, so pagination remains deterministic.

### Incident evidence

An evidence item is a typed, source-attributed investigation input, not a proven cause. The manual API accepts a summary, evidence type, source reference, and event time, then derives the submitter from the authenticated principal and assigns `HUMAN_ENTERED` provenance on the server. Clients cannot select or upgrade provenance; attempts to submit a provenance value are rejected.

Evidence is append-only in this increment and is returned through a bounded timeline ordered by event time and then opaque evidence ID. Event time cannot be later than the application recording time. Imported, retrieved, and model-generated provenance values are reserved for future controlled workflows; model output cannot enter human evidence through this manual endpoint.

### Document ingestion flow

1. The API validates size and allowed media type, calculates a checksum, stores the file, and creates a `SourceDocument` in `UPLOADED` state.
2. The knowledge application service extracts text and records `EXTRACTED` or a specific failure state.
3. It creates passages that preserve page/section and sequence metadata.
4. It generates embeddings in batches and writes passages plus vectors transactionally or with idempotent retry semantics.
5. The document becomes `INDEXED` only when all expected passages are searchable.

This may initially run after the upload request using an in-process task executor, with status polling. The persisted ingestion state and an idempotent retry operation must allow interrupted jobs to be recovered after a process restart. It must not hold a database transaction open during file extraction or remote model calls. Kafka is unnecessary at MVP volume; durable messaging becomes useful only when ingestion needs independent scaling, replay, or stronger failure isolation.

The milestone 7 implementation performs extraction and indexing synchronously after upload while preserving persisted state transitions: `UPLOADED` → `EXTRACTING` → `EXTRACTED` → `INDEXING` → `INDEXED`, with `EXTRACTION_FAILED` and `INDEXING_FAILED` as retryable failure states. Repository operations use short transactions; local storage reads, PDF processing, and embedding calls occur between them. Retry endpoints resume failed extraction or indexing, and re-uploading duplicate content resumes incomplete ingestion.

Passages never cross page boundaries. Text is whitespace-normalized and split into at most 800 characters with 120 characters of word-aligned overlap. Page number, document-wide sequence, text digest, embedding model, vector dimensions, indexing time, and a generated English full-text vector are retained. All embeddings are produced and validated before a single transaction replaces that document's passages, preventing partial batches or stale vectors from becoming searchable.

Milestone 18 retrieval obtains bounded exact-cosine and PostgreSQL full-text candidate rankings over `INDEXED` documents, with matching model and dimensions enforced on the semantic branch. The same optional document filter constrains both branches. Reciprocal rank fusion with rank constant 60 produces deterministic ordering without comparing incompatible raw score scales; the passage UUID breaks ties. Each branch receives three times the requested result count, capped at 60, and the normalized fusion score remains subject to the investigation relevance threshold. ADR 0004 records why approximate vector indexes remain deferred; ADR 0015 records hybrid ranking.

File identity is the SHA-256 checksum of its bytes. Re-uploading identical content returns the original document, including its original title, rather than creating another metadata record or stored file. The original filename is untrusted display metadata: path components are removed and the local storage key is generated from the opaque document ID. Storage defaults outside the web root and is configurable for deployment.

Only PDF and strict UTF-8 plain text are accepted. Direct PDFBox extraction retains non-empty pages and their one-based page numbers, bounded by configured upload, page, and extracted-character limits. Plain text is one logical page. Encrypted, malformed, scanned-only, or otherwise textless PDFs become `EXTRACTION_FAILED`; OCR is not attempted. ADR 0002 records the Tika/PDFBox comparison and selection.

### Grounded answering flow

1. Validate the investigation and permitted document scope.
2. Convert the bounded retrieval text into an embedding and normalized full-text lexemes.
3. Rank eligible passages semantically and lexically, apply the same document filters, and fuse the bounded rankings.
4. Build a bounded prompt containing instructions, the question, and numbered passages with provenance.
5. Ask the chat model to answer only from supplied passages and to report insufficient evidence when appropriate.
6. Resolve cited passage markers to application-owned `Citation` objects. Reject or downgrade malformed/unknown citations.
7. Return the answer and citations; retain safe metadata needed for later evaluation.

The milestone 8 implementation creates one idempotent investigation per incident and processes questions synchronously. A question is persisted as `PROCESSING` before retrieval and ends as `GROUNDED`, `INSUFFICIENT_EVIDENCE`, or `TECHNICAL_FAILURE`. Retrieval and model calls occur outside database transactions. The final state and immutable citation snapshots are stored atomically, and investigation responses return at most the latest 100 questions in chronological order.

Retrieval returns at most six passages and can be restricted to explicitly selected document IDs. Passages below the configured relevance threshold are excluded. Prompt version `grounded-answer-v3` includes bounded incident context and at most 12,000 characters of explicitly delimited, untrusted source data. It requires exactly one response block and one selected status. The provider adapter retries one rejected protocol response with a corrective instruction, then preserves the strict rejection boundary and records the attempted model if the retry also fails. Passage UUIDs remain reserved for the machine-readable citation field and are kept out of the human-readable answer; the provider adapter defensively removes echoed citation annotations. A generated answer is grounded only when every cited UUID belongs to the exact prompt passage set; missing or invented citations become a controlled technical failure. Insufficient retrieval bypasses the chat model entirely. ADR 0005 records the orchestration and validation policy.

The milestone 9 provider integration uses Spring AI's Ollama starter behind the existing embedding and answer ports. The default profile still selects deterministic adapters; only the explicit `ollama` profile creates local Ollama model clients. Automatic model pulling is disabled. Model tags and the base URL are environment-configurable, while evaluated development defaults target `qwen3.5:9b` with thinking disabled and `nomic-embed-text:latest`. Because vectors from different embedding models are incompatible, an already indexed document can be deliberately re-indexed; it is temporarily excluded from retrieval while its old passages are atomically replaced. ADR 0006 records this local-provider decision.

The milestone 10 hardening slice exposes only Actuator health with hidden details and explicit liveness/readiness groups, generates OpenAPI and Swagger UI from the application, and produces PlantUML diagrams plus module canvases from the verified Spring Modulith model during tests. The versioned synthetic evaluation set checks the deterministic retrieval baseline against expected documents, pages, relevance floors, and evidence terms. Generated model answers have an application-enforced length bound, and encrypted PDFs receive a controlled unsupported-input outcome. ADR 0007 records the operational documentation choices.

### Human-reviewed findings

The first milestone 11 slice adds an explicit boundary between generated decision support and accountable conclusions. A caller may propose one immutable draft finding from a `GROUNDED` question with validated citations, supplying a human-edited summary and proposer reference. Insufficient, failed, and still-processing answers are ineligible.

A draft has one terminal transition to `CONFIRMED` or `REJECTED`. Review requires the `REVIEWER` or `ADMIN` role and a rationale, and both proposal and review append immutable audit events attributed to the authenticated principal. Repeated review and silent editing are rejected. ADR 0008 records the review lifecycle; ADR 0012 records authenticated attribution and role boundaries.

An investigation closes only after all drafts are resolved and at least one finding is confirmed. A principal with `INVESTIGATOR` or `ADMIN` supplies a bounded case-level summary; the application derives the closer identity and records the closure time. Closure is terminal, so new questions, finding actions, and repeated closure are rejected. ADR 0009 records this lifecycle decision.

### Authentication and authorization boundary

Milestone 14 places Spring Security in the bootstrap layer because authentication is an application-wide delivery concern rather than an assets, incidents, knowledge, or investigations domain concept. Browser clients use a server-managed session and CSRF protection. Static login assets and health probes are public; business APIs, generated API documentation, and document content require authentication.

Local configuration supplies synthetic `INVESTIGATOR`, `REVIEWER`, and `ADMIN` users. Controllers derive human attribution from the authenticated principal before invoking module use cases, so business modules retain opaque actor references without depending on Spring Security types. The in-memory identity store is deliberately replaceable by OIDC for hosted use; it is not a user-management or multi-tenant authorization design. ADR 0012 records the decision.

### Repeatable RAG evaluation

Milestone 15 promotes the synthetic retrieval baseline into a versioned end-to-end quality gate. Fixture `v1` established the original seven-case baseline. Milestone 18 fixture `v3` preserves those cases and adds exact diagnostic-code retrieval for the hybrid ranker. The gate exercises document upload and indexing, deterministic retrieval, bounded prompt construction, answer-status classification, citation allow-listing, persistence, and the public question API. The default build requires every measured retrieval hit, grounded citation, expected status, context bound, and adversarial boundary to pass, then writes a Markdown report under `target/rag-evaluation/`.

An opt-in `v2` local-model comparison complements that deterministic gate without changing release behavior. It runs installed Ollama candidates through the real QIP answer adapter with identical bounded context and retrieved evidence, records objective protocol and provenance gates, and creates a blinded human scorecard for semantic quality. Model identities are kept in a separate reveal file to reduce reviewer bias; generated answers and reports remain ignored local artifacts because model output is not a repository fixture or human-confirmed evidence.

### Operational observability

Milestone 16 keeps diagnostics inside the modular monolith. A servlet boundary validates or creates X-Correlation-ID, returns it to the caller, places it in MDC for the request lifetime, and emits a structured completion event containing only method, path, status, and duration. Spring Boot's Logstash JSON console format makes those fields machine-readable without logging query strings, bodies, document content, prompts, model responses, credentials, or authorization headers.

Micrometer timers record extraction and indexing by fixed stage/outcome tags, retrieval by outcome, and model calls by outcome. A counter records persisted terminal answer status. IDs, users, and model names are excluded from metric tags to keep cardinality bounded. Actuator exposes only health and metrics; health details remain hidden, health stays public for probes, and metrics require the administrator role. An administrator-only Operations page converts those raw measurements into current-process counts, failure ratios, average/max latency, and answer outcomes with manual refresh. External telemetry, historical dashboards, and alerting remain deferred until hosted operation requires retention or cross-process tracing. ADR 0013 records this boundary.

Adversarial cases inject document instructions, a generated unsupported claim, and an invented citation. The answer adapter used to provoke those outputs exists only in test scope; production validation remains the system under test. A separate environment-gated Ollama comparison uses the three baseline cases and records configured model identifiers. It requires explicit invocation, performs no model downloads, and is not part of normal CI. This evaluation harness adds no production dependency or runtime service.

### AI concepts used in the MVP

- **Embedding:** a numeric representation of text in which semantically similar passages tend to be near one another. It solves vocabulary mismatch better than exact keyword search. It does not understand truth and is not an answer by itself. QIP combines this signal with PostgreSQL full-text ranking using deterministic reciprocal rank fusion.
- **Vector store:** storage plus an index for comparing embeddings. pgvector keeps this capability beside existing PostgreSQL data, avoiding another service in the MVP. A dedicated vector database is justified only by measured scale or operational needs.
- **Semantic search:** retrieving by meaning similarity using embeddings. It finds candidate evidence, but similarity is not proof of relevance.
- **RAG (retrieval-augmented generation):** retrieve passages first, then give only those passages to the model as context for an answer. It improves grounding and allows citations, but does not eliminate hallucinations.
- **Grounding:** tying an answer to supplied evidence. The platform enforces it through constrained prompts, explicit passage identifiers, citation validation, and an “insufficient evidence” path.
- **Tokens:** the model's units of input and output. Retrieved context, instructions, and answers all consume tokens, affecting cost, latency, and context limits. Passage count and size therefore need hard bounds.
- **Prompt injection:** malicious or accidental instructions inside uploaded content, such as “ignore previous rules.” Documents are untrusted data, never instructions. The prompt separates them clearly, tools are absent from MVP RAG, and output/citations are validated. Stronger defenses and adversarial tests are a later security milestone.

## 6. Technology decisions

The bootstrap pins Spring Boot 4.1.0, Spring Modulith 2.1.0, and Spring AI 2.0.0, compatible stable release lines selected in August 2026. QIP includes Spring AI's Ollama starter for explicit-profile local inference; domain and application code still depend only on application-owned ports, and other providers remain deployment choices.

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
GET    /api/documents/{documentId}/content
GET    /api/documents/{documentId}/status
POST   /api/documents/{documentId}/extraction
POST   /api/documents/{documentId}/indexing
GET    /api/documents?page=&size=

POST   /api/incidents/{incidentId}/investigations
POST   /api/investigations/{investigationId}/questions
GET    /api/investigations/{investigationId}
POST   /api/investigations/{investigationId}/findings
POST   /api/investigations/{investigationId}/findings/{findingId}/reviews
POST   /api/investigations/{investigationId}/closure
```

The question response includes answer status, answer text, citations, and model/retrieval metadata suitable for debugging without exposing secrets or hidden prompts.

Source inspection remains inside the knowledge-module boundary. The authenticated content endpoint resolves only the generated storage key owned by a known document, returns the original bytes inline with the recorded media type and `no-store` caching, and never returns a filesystem path or storage key. The web client links validated citations to that endpoint and adds the cited PDF page as a browser URL fragment, so page navigation does not expand the backend's authorization or storage surface.

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

9. **Local model provider integration**
   - Add an explicit Ollama profile for chat and embeddings through Spring AI.
   - Keep deterministic adapters as the default and prohibit automatic model downloads.
   - Support deliberate re-indexing when the active embedding model changes.
   - Add opt-in live smoke tests that require no database or paid credentials.

10. **Demo and MVP hardening**
   - Add synthetic seed data, public/sample documents, scripted demo path, OpenAPI docs, upload/security edge-case tests, health endpoints, and architecture diagrams generated from verified modules.
   - Define a small evaluation set before declaring the MVP complete.
   - Implemented with the synthetic loader and end-to-end investigation script, health probes, generated OpenAPI/Swagger UI, build-generated Modulith documentation, encrypted-upload and model-output hardening, and a versioned deterministic retrieval evaluation set.

11. **Human-reviewed findings and case lifecycle**
   - Require a separate attributed review before a draft finding becomes confirmed or rejected.
   - Close investigations only after drafts are resolved and a finding is confirmed; successful closure resolves the incident in the same application transaction.

12. **Incident evidence workspace**
   - Add bookmarkable incident records with paginated observation and human-entered evidence timelines.
   - Keep this structured case context out of model prompts until an explicit context-selection policy is designed and tested.

13. **Local runtime packaging**
   - Package the production React client and Spring Boot backend in one non-root image.
   - Start QIP and PostgreSQL through Compose while retaining host Ollama as an optional local adapter.

The v0.1.0 release-readiness pass presents the product and safety posture from the repository landing page, verifies the production image in CI, and publishes semantic-tagged JAR and GHCR artifacts with checksums and provenance attestations.

14. **Authenticated users and trustworthy attribution**
   - Add session-based browser authentication with configurable synthetic local users and CSRF protection.
   - Derive human actor references from the principal and enforce reviewer and investigator role boundaries.

15. **Repeatable RAG evaluation**
   - Turn the deterministic retrieval fixture into a complete, versioned grounded-answer quality gate.

16. **Operational observability**
   - Add safe structured correlation, latency/outcome metrics, an administrator-only current-process Operations page, and a bounded troubleshooting workflow.

17. **Hosted portfolio deployment**
   - Select and automate a modest deployment through an ADR without assuming distributed architecture.

The scope and acceptance goals for milestones 14–17 are maintained in [the post-v0.1 roadmap](roadmap.md).

## 11. Architectural decision policy

Record an ADR for choices that are costly to reverse, cross module boundaries, add an external service, or materially change security/operational behavior. Each ADR states context, decision, alternatives, consequences, and a reevaluation trigger.

A technology enters the project only when the current problem can be stated first, its simpler alternatives have been considered, and success can be measured. Portfolio value comes from being able to explain these decisions and trade-offs, not from maximizing the technology count.
