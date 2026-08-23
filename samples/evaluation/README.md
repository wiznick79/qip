# Synthetic grounded-answer evaluation

The current immutable case set is [`v1/rag-cases.csv`](v1/rag-cases.csv). A new directory is required for a material schema or scenario change; do not silently rewrite historical expectations to make a provider pass.

The seven offline cases cover three baseline retrieval questions, prompt injection embedded in an untrusted document, an unsupported generated claim, an invented citation, and insufficient retrieval. Expected pages, minimum relevance scores, and required evidence terms keep retrieval changes reviewable.

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
