# Synthetic grounded-answer evaluation

The current immutable offline case set is [`v3/rag-cases.csv`](v3/rag-cases.csv). The original [`v1` baseline](v1/rag-cases.csv) remains unchanged, while `v2` is reserved for the separate live-model comparison. A new directory is required for a material schema or scenario change; do not silently rewrite historical expectations to make a provider pass.

The eight offline cases cover three baseline retrieval questions, an exact diagnostic-code case for hybrid ranking, prompt injection embedded in an untrusted document, an unsupported generated claim, an invented citation, and insufficient retrieval. Expected pages, minimum relevance scores, and required evidence terms keep retrieval changes reviewable.

Run the deterministic quality gate with:

```powershell
.\scripts\run-rag-evaluation.ps1
```

It exercises upload, extraction, indexing, retrieval, bounded prompt construction, answer classification, citation allow-listing, persistence, and the public question API. It writes a human-readable report to `target/rag-evaluation/report.md`. The default build runs the same test without a network, model download, credentials, or paid calls.

The optional Ollama comparison is deliberately separate and limited to the three baseline cases:

```powershell
.\scripts\run-rag-evaluation.ps1 -Ollama
```

It requires a running local Ollama service and the explicitly configured chat and embedding models. Automatic model downloads remain disabled. Its report is `target/rag-evaluation/ollama-report.md`.

## Blinded local-model comparison

Fixture [`v2/model-comparison-cases.csv`](v2/model-comparison-cases.csv) expands the live comparison to twelve answer-quality scenarios. It covers supported actions, appropriate uncertainty, insufficient evidence, and instructions embedded in untrusted source text. The v2 fixture does not replace the immutable v1 release gate.

Run the default three-model comparison with an 8K context and thinking disabled:

```powershell
.\scripts\compare-ollama-models.ps1
```

The runner requires every named model to be installed, never pulls models, warms each model before measurement, and varies execution order to reduce ordering bias. Use `-Models`, `-Runs`, `-ContextLength`, and `-EmbeddingModel` to override its explicit defaults. One run produces 36 answers; after narrowing the candidates, `-Runs 3` can measure consistency without making the initial human review unnecessarily large.

Review `target/model-comparison/scorecard.md` without opening the identity key, then enter five scores from 0 to 2 for every response in `target/model-comparison/scores.csv`. Automated gates cover retrieval, expected answer status, citation validity, a non-empty response, and provider/protocol failures. Human scoring covers correctness, grounding, uncertainty, completeness, and clarity.

After scoring, reveal the identities in `target/model-comparison/reveal.md` and generate the ranked report:

```powershell
.\scripts\summarize-ollama-model-comparison.ps1
```

Raw provider results remain under `target/model-comparison/raw/`, which is generated and ignored by Git. The comparison intentionally uses QIP's Spring AI answer adapter rather than calling Ollama directly, so formatting retries and citation parsing match application behavior.
