package io.github.wiznick79.qip.investigations.internal.domain;

import io.github.wiznick79.qip.investigations.api.AnswerStatus;
import io.github.wiznick79.qip.investigations.api.CitationSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record InvestigationQuestion(
        UUID id,
        UUID investigationId,
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
    public InvestigationQuestion {
        selectedDocumentIds = Set.copyOf(selectedDocumentIds);
        citations = List.copyOf(citations);
    }
}
