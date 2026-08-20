package io.github.wiznick79.qip.knowledge.internal.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record KnowledgePassage(
        UUID id,
        UUID documentId,
        int sequence,
        int pageNumber,
        String text,
        String textSha256,
        Embedding embedding,
        String embeddingModel,
        Instant indexedAt) {

    public KnowledgePassage {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(documentId, "documentId is required");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be nonnegative");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        if (textSha256 == null || !textSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("textSha256 must be a lowercase SHA-256 digest");
        }
        Objects.requireNonNull(embedding, "embedding is required");
        if (embeddingModel == null || embeddingModel.isBlank() || embeddingModel.length() > 120) {
            throw new IllegalArgumentException("embeddingModel must contain 1 to 120 characters");
        }
        embeddingModel = embeddingModel.trim();
        Objects.requireNonNull(indexedAt, "indexedAt is required");
    }
}
