package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import io.github.wiznick79.qip.investigations.api.CitationSnapshot;
import java.util.UUID;

record CitationResponse(
        UUID passageId,
        UUID documentId,
        String documentTitle,
        int pageNumber,
        int passageSequence,
        String excerpt,
        double relevanceScore) {
    static CitationResponse from(CitationSnapshot citation) {
        return new CitationResponse(
                citation.passageId(),
                citation.documentId(),
                citation.documentTitle(),
                citation.pageNumber(),
                citation.passageSequence(),
                citation.excerpt(),
                citation.relevanceScore());
    }
}
