# QIP local MVP demonstration

This walkthrough uses only fictional machines and manuals committed to the repository. Nothing in the demo is suitable for real machinery.

## 1. Start dependencies and QIP

```powershell
docker compose up -d --wait
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,ollama"
```

Omit `ollama` to demonstrate the deterministic offline adapters. With Ollama, the configured models must already be installed; QIP never downloads them automatically.

## 2. Run the end-to-end case

In another PowerShell terminal:

```powershell
.\scripts\run-synthetic-investigation.ps1 -ReindexDocuments
```

The script idempotently loads three assets and manuals, verifies that the Atlas manual is indexed, creates a synthetic incident and investigation, asks one document-scoped question, and prints the answer status plus human-readable citation locators. Each run intentionally creates a new incident so its provenance and timestamps remain honest.

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
- Uploaded document and incident text are explicitly treated as untrusted data.
