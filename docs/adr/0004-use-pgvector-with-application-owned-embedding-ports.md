# ADR 0004: Use pgvector with application-owned embedding ports

- Status: Accepted
- Date: 2026-08-20

## Context

QIP needs searchable, page-attributed passages before it can build grounded answers. Indexing must be repeatable, must not expose model-provider types to domain code, and must remain testable without credentials, paid calls, or network access. The MVP data volume does not justify operating a separate vector database.

Embedding providers may produce vectors with different dimensions. Choosing a provider and dimension prematurely would make a provider switch a schema migration, while building an approximate index before representative data exists would add tuning and operational work without a measurable benefit.

## Decision

Split each extracted page independently into whitespace-normalized passages of at most 800 characters with 120 characters of word-aligned overlap. Retain the document ID, one-based page number, document-wide sequence, text SHA-256, embedding model ID, dimensions, and indexing time for every passage.

Store passages and embeddings in PostgreSQL using pgvector's unconstrained `vector` type. Replace all passages for one document in a single short transaction only after every embedding batch has succeeded and passed count and dimension validation. Search only documents in `INDEXED` state and only vectors produced by the query model with matching dimensions. Use exact cosine-distance search with bounded result counts for the MVP; do not add an HNSW or IVFFlat index until corpus size and latency measurements justify it.

Application code owns the `EmbeddingGenerator` and `PassageRepository` ports. The default adapter is a deterministic 64-dimensional feature-hashing fake for local development and automated tests. It is useful for repeatability and rough lexical similarity, not as a production semantic model. A profile-gated adapter uses Spring AI 2.0.0's provider-neutral `EmbeddingModel`; ADR 0006 configures its first provider through the explicit `ollama` profile.

Indexing extends the persisted document lifecycle to `EXTRACTED` → `INDEXING` → `INDEXED`, with `INDEXING_FAILED` as a retryable state. Extraction, embedding calls, and vector construction occur outside database transactions. Existing extracted documents can be indexed by re-uploading identical content or calling the indexing retry endpoint.

## Alternatives considered

### Spring AI vector-store abstraction

It can reduce provider-specific persistence code, but QIP needs explicit lifecycle filtering, provenance columns, atomic per-document replacement, and controlled SQL. A small JDBC adapter keeps those policies visible while Spring AI remains useful at the model boundary.

### Dedicated vector database

This adds another service, consistency boundary, credentials, and operational model. PostgreSQL already owns document state and pgvector supports the bounded MVP retrieval workload.

### PostgreSQL full-text search only

Keyword search would be simpler and remains a useful hybrid candidate. Embeddings are included because semantic retrieval is a core learning goal; the deterministic fake intentionally behaves more lexically in tests.

### Fixed-dimension vector column and approximate index

This enables pgvector's approximate indexes, but locks the schema to one embedding dimension. The MVP first needs a selected production model, representative corpus, and measured exact-search latency.

## Consequences

- Passage provenance and model metadata remain inspectable and citation-ready.
- Failed or partial embedding batches cannot delete a document's previous searchable passages.
- Normal builds and tests remain offline and deterministic.
- Exact search will eventually become too slow as the passage corpus grows.
- Changing chunking or embedding models requires explicit re-indexing; mixed model spaces are never compared.
- Provider starters and credentials are intentionally not included in this milestone.

## Reevaluation triggers

Select a fixed embedding dimension and add a pgvector index when a production embedding model is chosen and measured exact-search latency exceeds the retrieval budget. Reconsider hybrid full-text/vector ranking when evaluation shows vocabulary-heavy queries are missed. Reconsider a dedicated vector service only when measured scale, independent lifecycle, or operational requirements exceed PostgreSQL's practical limits.

## References

- [Spring AI embedding model API](https://docs.spring.io/spring-ai/reference/api/embeddings.html)
- [pgvector](https://github.com/pgvector/pgvector)
