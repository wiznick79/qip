package io.github.wiznick79.qip.knowledge.api;

import java.util.UUID;

public record RetrievedPassage(
        UUID passageId,
        UUID documentId,
        String documentTitle,
        int pageNumber,
        int sequence,
        String text,
        double score) {}
