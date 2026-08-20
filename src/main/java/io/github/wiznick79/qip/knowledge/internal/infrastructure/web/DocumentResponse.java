package io.github.wiznick79.qip.knowledge.internal.infrastructure.web;

import io.github.wiznick79.qip.knowledge.api.DocumentSnapshot;
import java.time.Instant;
import java.util.UUID;

record DocumentResponse(
        UUID id,
        String title,
        String originalFilename,
        String mediaType,
        long sizeBytes,
        String checksumSha256,
        String status,
        String failureReason,
        int extractedPageCount,
        Instant uploadedAt,
        Instant updatedAt) {

    static DocumentResponse from(DocumentSnapshot document) {
        return new DocumentResponse(
                document.id(),
                document.title(),
                document.originalFilename(),
                document.mediaType().value(),
                document.sizeBytes(),
                document.checksumSha256(),
                document.status().name(),
                document.failureReason(),
                document.extractedPageCount(),
                document.uploadedAt(),
                document.updatedAt());
    }
}
