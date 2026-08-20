package io.github.wiznick79.qip.knowledge.api;

import java.util.Set;
import java.util.UUID;

public record KnowledgeQuery(String text, Set<UUID> documentIds, int limit) {

    public KnowledgeQuery {
        if (text == null || text.isBlank() || text.length() > 1_000) {
            throw new IllegalArgumentException("text must contain 1 to 1000 characters");
        }
        text = text.trim();
        documentIds = documentIds == null ? Set.of() : Set.copyOf(documentIds);
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("limit must be between 1 and 20");
        }
    }
}
