package io.github.wiznick79.qip.investigations.api;

import java.util.UUID;

public record CitationSnapshot(
        UUID passageId,
        UUID documentId,
        String documentTitle,
        int pageNumber,
        int passageSequence,
        String excerpt,
        double relevanceScore) {}
