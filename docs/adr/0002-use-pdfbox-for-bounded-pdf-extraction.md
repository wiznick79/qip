# ADR 0002: Use PDFBox for bounded PDF extraction

- Status: Accepted
- Date: 2026-08-20

## Context

QIP's first ingestion increment accepts only digitally generated PDF and UTF-8 plain-text documents. It must retain page locators for later citations, reject malformed or oversized inputs predictably, and keep extraction replaceable. OCR and broad office-format support are explicitly outside the MVP.

The extraction spike compared Apache PDFBox with Apache Tika. Tika provides a uniform parser interface and detects or extracts more than one thousand formats, which is useful when a product genuinely supports a broad document set. Its standard parser package also introduces many transitive dependencies. PDFBox directly extracts Unicode text from PDF pages, while the JDK is sufficient for strict UTF-8 text decoding.

## Decision

Use Apache PDFBox 3.0.8 behind the knowledge module's application-owned `TextExtractor` port. Extract PDF pages independently so page numbers remain available for later passage citations. Decode plain text with the JDK's strict UTF-8 decoder rather than routing it through PDFBox.

Bound extraction to 500 PDF pages and 5,000,000 extracted characters by default. Reject encrypted PDFs, scanned PDFs without extractable text, malformed files, and limits breaches with a persisted `EXTRACTION_FAILED` status. Do not enable OCR in this milestone.

## Alternatives considered

### Apache Tika standard parsers

Tika has a strong unified abstraction and is the better choice when supported formats expand materially. For only PDF and plain text, its broad parser package and transitive dependency surface are unnecessary maintenance and security costs.

### PDFBox plus an OCR engine

OCR would support scanned documents, but adds native/runtime dependencies, slower processing, more resource controls, and a larger untrusted-input surface. The MVP explicitly excludes it.

### Implement PDF parsing directly

PDF is too complex and security-sensitive for an application-specific parser. Reusing a maintained library is safer and substantially less costly.

## Consequences

- Page-level provenance is available for the later chunking and citation milestones.
- The dependency surface matches the deliberately narrow upload contract.
- Broad document formats and OCR remain unsupported and fail visibly.
- PDFBox exceptions do not cross the application port; callers see controlled extraction outcomes.
- Upgrades require dependency review and the PDF extraction contract tests.

## Reevaluation triggers

Reconsider Tika or dedicated format adapters if the product accepts office documents or several additional media types. Reconsider OCR only with a concrete scanned-document use case, representative synthetic fixtures, resource limits, and an updated threat model.

## References

- [Apache PDFBox](https://pdfbox.apache.org/)
- [Apache Tika](https://tika.apache.org/)
