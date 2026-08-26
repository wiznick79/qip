# ADR 0006: Use Ollama for opt-in local model inference

- Status: Accepted
- Date: 2026-08-20

## Context

QIP's grounded-answer and embedding ports have deterministic adapters, but those adapters demonstrate orchestration rather than semantic retrieval or natural-language synthesis. Testing with a hosted provider would require credentials and paid usage. A local Ollama installation already provides `qwen3-coder:30b` for completion and `nomic-embed-text:latest` for embeddings.

Adding a provider must not make normal development, CI, or tests depend on a running model server. Existing documents may contain vectors produced by another model and must not be mixed with query vectors from Ollama.

## Decision

Add Spring AI 2.0's `spring-ai-starter-model-ollama` dependency because it provides both `ChatModel` and `EmbeddingModel` implementations behind QIP's existing application-owned ports. Activate those adapters only under an explicit `ollama` Spring profile. Keep deterministic adapters active in every other profile.

Default the local base URL to `http://localhost:11434`, chat model to `qwen3-coder:30b`, and embedding model to `nomic-embed-text:latest`. Allow environment-variable overrides. Never automatically pull models at application startup; operators install and assess model artifacts deliberately.

Bound HTTP connection attempts to five seconds and model response reads to three minutes by default, both configurable for slower hardware. Limit transient provider retries to two short attempts so a local outage does not hold an application request through Spring AI's much longer generic retry defaults.

Continue to treat all provider output as untrusted. The application parses a narrow response protocol, accepts optional Markdown fencing only, validates every citation UUID against the exact retrieved passage set, and converts provider or format errors into controlled technical failures. If a response violates the protocol, retry once with an explicit correction that requires one response block; reject the second invalid response without exposing either raw provider payload, while retaining the attempted model identifier for diagnostics.

Permit the existing indexing operation to deliberately re-index an `INDEXED` document. The document enters `INDEXING`, making old passages ineligible for retrieval, and the new passage/vector set atomically replaces the old set only after all embeddings succeed. This supports switching from deterministic to Ollama embeddings without stale-vector mixing.

Keep live-model tests opt-in through `QIP_LIVE_MODEL_TEST=true`. Isolate them from PostgreSQL and verify both completion protocol handling and finite embedding output. Default verification skips these tests and remains network- and credential-free.

Request an explicit 8,192-token chat context by default rather than inheriting a model's native maximum. QIP supplies at most 12,000 evidence characters and permits at most 800 generated tokens, while newer models may advertise 256K contexts whose KV cache forces otherwise suitable models out of limited VRAM. Keep the context configurable and use `ollama ps` when selecting a hardware-specific override.

## Alternatives considered

### Hosted OpenAI or Azure OpenAI first

Both are supported by the port design, but they require credentials and paid usage. They can be evaluated later without changing domain or application code.

### Call Ollama's HTTP API directly

This would avoid the starter's transitive dependencies but duplicate request mapping, model configuration, error translation, and future provider-neutral behavior already supplied by Spring AI. The starter's maintenance cost is acceptable because QIP already depends on Spring AI model interfaces.

### Make Ollama the default profile

This would make application startup and normal tests depend on a local process and large model artifacts. An explicit profile preserves deterministic, reproducible development and CI.

### Automatically pull missing models

Automatic downloads can be very large, slow, and operationally surprising. QIP configures the pull strategy as `never` and fails visibly when a selected model is unavailable.

## Consequences

- QIP can exercise real local embeddings and grounded synthesis without API keys or per-request fees.
- First model load can be slow and local performance depends on available RAM, VRAM, and CPU.
- The Ollama starter adds HTTP/reactive client transitive dependencies to the single application module.
- Changing embedding models requires re-indexing documents before they are searchable with the new model.
- A provider failure remains a persisted `TECHNICAL_FAILURE`; raw provider payloads are not exposed.
- Hosted providers remain replaceable adapters rather than domain dependencies.

## Reevaluation triggers

Evaluate another chat model when the synthetic evaluation set shows weak groundedness, formatting reliability, or latency. Add a hosted provider when deployment requirements justify credentials and usage cost. Reconsider synchronous answering when measured local-model latency exceeds the HTTP request budget.

## References

- [Spring AI Ollama chat](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- [Spring AI Ollama embeddings](https://docs.spring.io/spring-ai/reference/api/embeddings/ollama-embeddings.html)
