# ADR 0005: Orchestrate grounded answers with application-owned validation

- Status: Accepted
- Date: 2026-08-20

## Context

QIP now stores indexed, page-attributed passages. The next requirement is to answer an investigator's question without turning retrieval similarity or model output into an autonomous root-cause judgment. Uploaded documents and incident descriptions are untrusted data. Model responses may be malformed, cite invented identifiers, or fail after a question has already been accepted.

The feature must remain demonstrable without network access or provider credentials, while leaving a narrow boundary for a real chat model. Prompt contents affect persisted behavior and therefore need a traceable version. Question history and citations must survive later changes to passages or model configuration.

## Decision

Create one idempotent investigation per incident. Persist each question in `PROCESSING` before retrieval or model work, then complete it as exactly one of `GROUNDED`, `INSUFFICIENT_EVIDENCE`, or `TECHNICAL_FAILURE`. Keep model and retrieval calls outside database transactions; final answer metadata and citation snapshots are written atomically.

Retrieve at most six passages, optionally constrained to selected document IDs. Discard passages below the configured minimum relevance score. Build prompt version `grounded-answer-v2` with at most 12,000 source characters. Incident context and source passages are explicitly delimited as untrusted data. The prompt gives the model no tools and instructs it to return either insufficient evidence or a concise answer, with passage UUIDs isolated in the machine-readable citation field. The provider adapter defensively removes echoed UUID citation annotations from the human-readable answer.

An answer becomes `GROUNDED` only when it is nonblank, bounded, has a valid model identifier, and cites at least one passage from the exact context sent to the generator. Unknown citation IDs make the result `TECHNICAL_FAILURE`; they are never returned as sources. Citation rows snapshot document title, page, sequence, excerpt, and relevance score so answer provenance remains readable.

Use a deterministic extractive fake answer generator by default. It cites the leading retrieved passages and is intended for workflow and contract testing, not semantic synthesis. A profile-scoped adapter calls Spring AI 2.0's provider-neutral `ChatModel` and parses the same controlled response protocol. ADR 0006 selects Ollama as the first opt-in provider without changing this orchestration policy.

## Alternatives considered

### Return retrieval results without an answer

This would prove search but not the grounded-answer workflow, citation validation, or insufficient-evidence behavior that the product exists to demonstrate.

### Trust model-produced citations

Models can invent identifiers even under strong prompting. Accepting them would break provenance. Application-side allow-list validation is deterministic and provider-independent.

### Let the model query databases or call tools

This broadens authorization and prompt-injection risk without an MVP use case. The model receives only explicitly selected, bounded text and has no SQL, HTTP, filesystem, or tool access.

### Persist the full prompt and model response

This simplifies debugging but unnecessarily retains document bodies and potentially sensitive content. QIP persists a prompt version and safe diagnostics instead.

## Consequences

- Grounded status has an enforceable meaning rather than being a model assertion.
- Insufficient retrieval avoids a model call and produces a stable user-visible outcome.
- Question failures remain visible without leaking provider payloads.
- Citation snapshots preserve answer provenance if passage storage later changes.
- Synchronous answering is simple but ties request latency to retrieval and model latency.
- The deterministic fake demonstrates control flow and provenance, not production answer quality.
- Only the latest 100 questions are returned in the first workspace response; explicit pagination is a later API refinement.

## Reevaluation triggers

Move question execution to durable asynchronous work when provider latency exceeds the HTTP budget or interrupted requests become operationally significant. Add structured provider-native output when a production provider is selected and evaluated. Add incident observations/evidence to model context only through a separate bounded, provenance-aware design. Add question pagination when investigation histories approach 100 entries.

## References

- [Spring AI Chat Model API](https://docs.spring.io/spring-ai/reference/api/chatmodel.html)
- [Spring AI structured output](https://docs.spring.io/spring-ai/reference/api/structured-output.html)
