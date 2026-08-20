package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.internal.domain.SourceDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {
    SourceDocument save(SourceDocument document);

    SourceDocument saveExtraction(SourceDocument document, List<ExtractedPage> pages);

    Optional<SourceDocument> findById(UUID documentId);

    Optional<SourceDocument> findByChecksum(String checksumSha256);

    int extractedPageCount(UUID documentId);
}
