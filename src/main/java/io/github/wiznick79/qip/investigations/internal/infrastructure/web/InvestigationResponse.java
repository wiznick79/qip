package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import io.github.wiznick79.qip.investigations.api.InvestigationSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record InvestigationResponse(
        UUID id,
        UUID incidentId,
        List<QuestionAnswerResponse> questions,
        List<FindingResponse> findings,
        Instant createdAt,
        Instant updatedAt) {
    static InvestigationResponse from(InvestigationSnapshot investigation) {
        return new InvestigationResponse(
                investigation.id(),
                investigation.incidentId(),
                investigation.questions().stream()
                        .map(QuestionAnswerResponse::from)
                        .toList(),
                investigation.findings().stream().map(FindingResponse::from).toList(),
                investigation.createdAt(),
                investigation.updatedAt());
    }
}
