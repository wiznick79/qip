package io.github.wiznick79.qip.knowledge.internal.infrastructure.web;

import io.github.wiznick79.qip.knowledge.api.DocumentSnapshot;
import java.time.Instant;
import java.util.UUID;

record DocumentStatusResponse(
        UUID documentId, String status, String failureReason, int extractedPageCount, Instant updatedAt) {

    static DocumentStatusResponse from(DocumentSnapshot document) {
        return new DocumentStatusResponse(
                document.id(),
                document.status().name(),
                document.failureReason(),
                document.extractedPageCount(),
                document.updatedAt());
    }
}
