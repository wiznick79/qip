package io.github.wiznick79.qip.knowledge.api;

import java.time.Instant;
import java.util.UUID;

public record DocumentSnapshot(
        UUID id,
        String title,
        String originalFilename,
        DocumentMediaType mediaType,
        long sizeBytes,
        String checksumSha256,
        DocumentStatus status,
        String failureReason,
        int extractedPageCount,
        Instant uploadedAt,
        Instant updatedAt) {}
