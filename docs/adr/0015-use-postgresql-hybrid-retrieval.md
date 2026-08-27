# ADR 0015: Fuse PostgreSQL lexical and semantic passage rankings

- Status: accepted
- Date: 2026-08-27

## Context

QIP's exact pgvector cosine search handles vocabulary variation, but identifiers, alarm codes, measurements, and component names are often better lexical signals. Replacing semantic search with keyword search would lose that vocabulary tolerance. Comparing or summing raw cosine similarity and PostgreSQL text rank would also be misleading because the two scores have different, query-dependent scales.

Retrieval must remain bounded, deterministic, document-scoped, and internal to the knowledge module. It must not weaken citation provenance or introduce another runtime service.

## Decision

For each knowledge query, retrieve two independently ordered candidate lists from the same eligible `INDEXED` passages:

1. exact cosine similarity for passages using the active embedding model and dimensions;
2. PostgreSQL English full-text search over a stored generated `tsvector`, accelerated by a GIN index.

Convert normalized query lexemes into an OR expression so natural-language incident context does not require every word to occur in one passage. Apply the same optional document IDs and candidate bound to both searches.

Fuse the lists in the knowledge application service with reciprocal rank fusion using rank constant 60. Request three times the public result limit from each ranking, capped at 60 candidates per ranking. A passage receives one reciprocal-rank contribution from each list in which it appears. Sort by the summed contribution, then passage UUID for deterministic ties, and return only the requested count.

Expose a normalized fusion score from zero to one by dividing by the theoretical score of rank one in both lists. Keep the existing passage identity, source metadata, result bounds, downstream relevance gate, prompt bound, and citation allow-list unchanged. A failed embedding or database operation remains a retrieval failure; lexical-only failover is not introduced silently.

## Alternatives considered

### Vector search only

This is simpler and remains useful, but it can under-rank exact industrial identifiers and measurements that have high diagnostic value.

### PostgreSQL full-text search only

This handles exact vocabulary well but loses semantic matching when questions and manuals use different language.

### Weighted addition of raw scores

Cosine similarity and text rank do not share a stable scale. Weights would encode accidental properties of the current embedding model and document corpus, making model changes difficult to interpret.

### Dedicated search or vector service

Elasticsearch, OpenSearch, or a dedicated vector database could provide more ranking controls at the cost of another deployment, data synchronization, and operational boundary. Current scale provides no evidence that PostgreSQL is insufficient.

## Consequences

- Exact codes and keywords can reinforce semantically relevant passages without sacrificing semantic recall.
- Existing document filters, provenance, and citation identifiers continue through one knowledge API.
- Ranking is deterministic and independent of raw-score scale, but the exposed relevance value now represents normalized fusion strength rather than cosine similarity.
- Every question performs two bounded database searches and still requires a query embedding.
- English text configuration is an explicit current limitation; multilingual stemming requires evaluation and a later decision.
- Flyway migration 11 computes stored search vectors for existing passages and adds a GIN index.

## Re-evaluation trigger

Revisit the rank constant, candidate multiplier, language configuration, or retrieval backend only when a versioned evaluation set demonstrates a quality regression, measured corpus/query volume shows unacceptable latency, or multilingual evidence becomes a supported requirement.
