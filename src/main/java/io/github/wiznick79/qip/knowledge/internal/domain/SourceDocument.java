package io.github.wiznick79.qip.knowledge.internal.domain;

import io.github.wiznick79.qip.knowledge.api.DocumentMediaType;
import io.github.wiznick79.qip.knowledge.api.DocumentStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SourceDocument(
        UUID id,
        String title,
        String originalFilename,
        DocumentMediaType mediaType,
        long sizeBytes,
        String checksumSha256,
        String storageKey,
        DocumentStatus status,
        String failureReason,
        Instant uploadedAt,
        Instant updatedAt) {

    public SourceDocument {
        Objects.requireNonNull(id, "id is required");
        title = requiredText(title, "title", 200);
        originalFilename = requiredText(originalFilename, "originalFilename", 255);
        Objects.requireNonNull(mediaType, "mediaType is required");
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
        checksumSha256 = requiredText(checksumSha256, "checksumSha256", 64);
        if (!checksumSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("checksumSha256 must be a lowercase SHA-256 digest");
        }
        storageKey = requiredText(storageKey, "storageKey", 100);
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(uploadedAt, "uploadedAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
        if (updatedAt.isBefore(uploadedAt)) {
            throw new IllegalArgumentException("updatedAt must not precede uploadedAt");
        }
        if (status == DocumentStatus.EXTRACTION_FAILED) {
            failureReason = requiredText(failureReason, "failureReason", 500);
        } else if (failureReason != null) {
            throw new IllegalArgumentException("failureReason is only valid for failed extraction");
        }
    }

    public SourceDocument startExtraction(Instant now) {
        if (status == DocumentStatus.EXTRACTED) {
            return this;
        }
        if (status != DocumentStatus.UPLOADED && status != DocumentStatus.EXTRACTION_FAILED) {
            throw new InvalidDocumentStateException(status);
        }
        return withState(DocumentStatus.EXTRACTING, null, now);
    }

    public SourceDocument completeExtraction(Instant now) {
        requireExtracting();
        return withState(DocumentStatus.EXTRACTED, null, now);
    }

    public SourceDocument failExtraction(String reason, Instant now) {
        requireExtracting();
        return withState(DocumentStatus.EXTRACTION_FAILED, reason, now);
    }

    private void requireExtracting() {
        if (status != DocumentStatus.EXTRACTING) {
            throw new InvalidDocumentStateException(status);
        }
    }

    private SourceDocument withState(DocumentStatus newStatus, String reason, Instant now) {
        return new SourceDocument(
                id,
                title,
                originalFilename,
                mediaType,
                sizeBytes,
                checksumSha256,
                storageKey,
                newStatus,
                reason,
                uploadedAt,
                now);
    }

    private static String requiredText(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name + " is required");
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " must contain 1 to " + maxLength + " characters");
        }
        return trimmed;
    }
}
