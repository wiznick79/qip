package io.github.wiznick79.qip.investigations.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvestigationSnapshot(
        UUID id,
        UUID incidentId,
        List<QuestionAnswerSnapshot> questions,
        List<FindingSnapshot> findings,
        Instant createdAt,
        Instant updatedAt) {
    public InvestigationSnapshot {
        questions = List.copyOf(questions);
        findings = List.copyOf(findings);
    }
}
