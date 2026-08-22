# QIP local MVP demonstration

This walkthrough uses only fictional machines and manuals committed to the repository. Nothing in the demo is suitable for real machinery.

## 1. Start the complete stack

```powershell
docker compose up --build -d --wait
```

This starts PostgreSQL/pgvector and the single QIP application image containing both the React production bundle and Spring Boot API. Ollama remains on the host and must already be running with the configured models; QIP never downloads them automatically. Set `QIP_SPRING_PROFILES=local` in `.env` to demonstrate the deterministic offline adapters instead.

## 2. Run the end-to-end case

In another PowerShell terminal:

```powershell
.\scripts\run-synthetic-investigation.ps1 -ReindexDocuments
```

The script idempotently loads three assets and manuals, verifies that the Atlas manual is indexed, creates a synthetic incident with one attributed observation and one server-labelled `HUMAN_ENTERED` measurement, starts its investigation, and explicitly moves the incident to `INVESTIGATING` before asking one document-scoped question. The observation and evidence remain visible incident records and are not sent to the model. When the answer is grounded, it proposes a draft finding, records a separate synthetic human review, closes the investigation with a human-authored summary, and separately marks the incident `RESOLVED`. It then prints the incident status, observation and evidence IDs, answer, citation, finding status, audit-event count, and closure record. Each run intentionally creates a new incident so its provenance and timestamps remain honest.

After the first Ollama re-index, omit `-ReindexDocuments` on later runs.

## 3. Inspect the system

- Web client: `http://localhost:8080/`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
- Liveness: `http://localhost:8080/actuator/health/liveness`
- Readiness: `http://localhost:8080/actuator/health/readiness`

The generated Spring Modulith diagrams and canvases are written to `target/spring-modulith-docs/` during the Maven test suite.

## Expected safety behavior

- Answers are decision support, not confirmed root-cause findings.
- A grounded answer includes only citations selected from the exact retrieved passage set.
- Weak retrieval produces `INSUFFICIENT_EVIDENCE` without calling the chat model.
- Malformed model output, invented citations, and provider failures become controlled technical failures.
- Only grounded answers with citations can be proposed as draft findings.
- Findings remain unconfirmed until a separate reviewer records a rationale; reviewed findings are immutable.
- Closure requires no unresolved drafts and at least one confirmed finding; closed investigations are immutable.
- Investigation closure and incident resolution are separate transitions; the demo performs both explicitly and finishes with incident status `RESOLVED`.
- Uploaded document and incident text are explicitly treated as untrusted data.
