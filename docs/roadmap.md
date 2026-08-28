# QIP roadmap

The first portfolio release proves the complete local investigation workflow. The next milestones harden that workflow without changing QIP's modular-monolith boundary or turning decision support into autonomous root-cause judgment.

## Milestone 14 — authenticated users and trustworthy attribution

Replace caller-supplied identity labels with an authenticated application principal.

- Add Spring Security and a browser login/logout flow.
- Provide synthetic local users with `INVESTIGATOR`, `REVIEWER`, and `ADMIN` roles.
- Derive observation, evidence, finding-review, and closure attribution from the authenticated principal.
- Require the reviewer role for confirming or rejecting findings.
- Require the investigator role for closing an investigation.
- Return stable RFC 9457 responses for unauthenticated and forbidden API requests.
- Keep local credentials configurable and document that defaults are development-only.
- Cover successful authentication, invalid credentials, and role boundaries with backend and frontend tests.

Enterprise identity, user administration, password recovery, multi-tenancy, and plant-level authorization remain outside this milestone.

## Milestone 15 — repeatable RAG evaluation

Turn the existing synthetic retrieval fixture into a versioned quality gate for the complete grounded-answer pipeline.

- Measure retrieval hit rate, citation validity, grounded/insufficient classification, and context bounds.
- Add adversarial fixtures for prompt injection, unsupported claims, and unknown citations.
- Produce a human-readable evaluation report without requiring a live model in the default build.
- Keep optional Ollama evaluation separate, inexpensive, and explicitly enabled.

Implemented with versioned fixture `v1`, a deterministic full-pipeline quality gate and Markdown report, adversarial output cases, and an explicitly enabled three-case Ollama comparison.

## Milestone 16 — operational observability

Make ingestion and model behavior diagnosable without logging document bodies, prompts, model responses, or secrets.

- Add structured, correlation-aware application logs.
- Record ingestion duration and outcome, retrieval duration, model latency, and answer status metrics.
- Expose bounded operational metrics and an administrator-only current-process dashboard while keeping sensitive health details hidden.
- Document a small local troubleshooting workflow and service-level indicators.

An external observability stack is added only when deployment needs justify it.

Implemented with validated request correlation IDs, structured safe request logs, low-cardinality Micrometer timers and counters for ingestion/retrieval/model/answer outcomes, administrator-only Actuator metrics and Operations page, hidden health details, and a local troubleshooting and SLI guide.

## Milestone 17 — hosted portfolio deployment

Define and automate a modest hosted deployment for the released container.

- Select a hosting target through an ADR based on cost, operational burden, persistence, and Ollama/provider constraints.
- Keep database, document storage, secrets, backups, TLS, and image provenance explicit.
- Add automated deployment and rollback from tagged releases.
- Publish a safe synthetic demonstration, or document why an on-demand/private demo is preferable.

This milestone does not justify Kubernetes, microservices, Kafka, or a separate frontend deployment by itself.

Implemented with ADR 0014, a portable single-VM Compose topology targeting a 2 GB Lightsail instance, Caddy-managed TLS, private application/database networking, separate persistent volumes, host-only secrets, bounded backups, attested semantic-release deployment, readiness-based automatic rollback, and an explicit private/on-demand synthetic-demo policy.

## v0.2.0 release boundary

The second portfolio release contains milestones 14–17: authenticated attribution and role boundaries, repeatable RAG evaluation, bounded operational observability, and an automated single-VM deployment path. The workload-specific local-model comparison also selects `qwen3.5:9b` as a configurable development default without making live model access part of the normal build.

## Milestone 18 — hybrid retrieval and cited-source inspection

Improve evidence discovery and let users inspect an answer's source in context while retaining QIP's existing provenance and safety boundaries.

- Combine the existing pgvector semantic ranking with PostgreSQL full-text ranking behind the knowledge module's search API.
- Fuse rankings deterministically, preserve explicit document filters and result bounds, and extend the versioned evaluation gate before changing defaults.
- Let authenticated users open a cited source document at the cited PDF page from the investigation workspace.
- Authorize document access through the application API; never expose storage paths or bypass the knowledge module.
- Reuse the blinded Ollama comparison for future local-model changes rather than treating model size or generic benchmarks as sufficient evidence.

OCR, a dedicated search service or vector database, autonomous investigation actions, and unrestricted document browsing remain outside this milestone.

Hybrid retrieval is implemented behind the knowledge API with bounded PostgreSQL full-text and exact pgvector candidate lists, deterministic reciprocal-rank fusion, identical document filters, Flyway-managed GIN indexing, and the versioned `v3` offline quality gate. Authenticated citations now open source content through the knowledge API, with PDF links targeting the cited page through native browser navigation while storage keys and paths remain private. These two slices complete Milestone 18.

## Milestone 19 - closed-investigation PDF reports

Turn a completed investigation into a portable, attributable case record without creating another mutable source of truth.

- Export only closed investigations through an authenticated application endpoint.
- Include asset and incident context, observations, evidence, grounded questions and citations, reviewed findings and audit events, and immutable closure details.
- Generate the PDF on demand with deterministic layout, safe wrapping and pagination, stable filenames, page numbers, and non-cached delivery.
- Add the export action to the closed investigation workspace and verify both document content and rendered layout with synthetic data.

Digital signatures, email delivery, stored report versions, arbitrary templates, and exports from open investigations remain outside this milestone.
