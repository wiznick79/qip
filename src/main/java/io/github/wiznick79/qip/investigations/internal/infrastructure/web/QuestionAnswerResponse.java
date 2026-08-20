package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import io.github.wiznick79.qip.investigations.api.AnswerStatus;
import io.github.wiznick79.qip.investigations.api.QuestionAnswerSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

record QuestionAnswerResponse(
        UUID id,
        String question,
        Set<UUID> selectedDocumentIds,
        AnswerStatus status,
        String answer,
        List<CitationResponse> citations,
        String modelId,
        String promptVersion,
        int retrievedPassageCount,
        String failureReason,
        Instant askedAt,
        Instant completedAt) {
    static QuestionAnswerResponse from(QuestionAnswerSnapshot question) {
        return new QuestionAnswerResponse(
                question.id(),
                question.question(),
                question.selectedDocumentIds(),
                question.status(),
                question.answer(),
                question.citations().stream().map(CitationResponse::from).toList(),
                question.modelId(),
                question.promptVersion(),
                question.retrievedPassageCount(),
                question.failureReason(),
                question.askedAt(),
                question.completedAt());
    }
}
