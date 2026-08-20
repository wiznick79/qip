# Synthetic grounded-QA evaluation set

`grounded-qa.csv` is the small, versioned MVP regression set for retrieval. Every case points to one fictional PDF, an expected source page, a minimum deterministic relevance score, and terms that must occur in the highest-ranked passage.

The default build evaluates the offline `deterministic-hash-v1` adapter so CI needs no network, model download, credentials, or paid calls. This is a lexical retrieval baseline, not a claim about answer quality. Insufficient-evidence, citation allow-listing, malformed output, prompt injection, timeouts, and provider failure remain covered by focused orchestration/adapter tests.

When comparing Ollama or another provider, use the same case IDs and record model tags, retrieval results, latency, answer status, and citation validity. Do not silently change expected pages or terms to make a model pass; review changes alongside the synthetic manuals.
