package io.github.wiznick79.qip.investigations.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FindingSnapshot(
        UUID id,
        UUID sourceQuestionId,
        String summary,
        FindingStatus status,
        String proposedBy,
        Instant proposedAt,
        String reviewedBy,
        String reviewRationale,
        Instant reviewedAt,
        List<FindingReviewEventSnapshot> events) {
    public FindingSnapshot {
        events = List.copyOf(events);
    }
}
