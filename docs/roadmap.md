# QIP roadmap after v0.1.0

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
- Expose bounded operational metrics while keeping sensitive health details hidden.
- Document a small local troubleshooting workflow and service-level indicators.

An external observability stack is added only when deployment needs justify it.

## Milestone 17 — hosted portfolio deployment

Define and automate a modest hosted deployment for the released container.

- Select a hosting target through an ADR based on cost, operational burden, persistence, and Ollama/provider constraints.
- Keep database, document storage, secrets, backups, TLS, and image provenance explicit.
- Add automated deployment and rollback from tagged releases.
- Publish a safe synthetic demonstration, or document why an on-demand/private demo is preferable.

This milestone does not justify Kubernetes, microservices, Kafka, or a separate frontend deployment by itself.
