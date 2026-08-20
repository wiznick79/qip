package io.github.wiznick79.qip.investigations.api;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record QuestionAnswerSnapshot(
        UUID id,
        String question,
        Set<UUID> selectedDocumentIds,
        AnswerStatus status,
        String answer,
        List<CitationSnapshot> citations,
        String modelId,
        String promptVersion,
        int retrievedPassageCount,
        String failureReason,
        Instant askedAt,
        Instant completedAt) {
    public QuestionAnswerSnapshot {
        selectedDocumentIds = Set.copyOf(selectedDocumentIds);
        citations = List.copyOf(citations);
    }
}
