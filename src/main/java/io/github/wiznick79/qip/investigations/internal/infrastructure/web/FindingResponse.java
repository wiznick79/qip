package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import io.github.wiznick79.qip.investigations.api.FindingSnapshot;
import io.github.wiznick79.qip.investigations.api.FindingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record FindingResponse(
        UUID id,
        UUID sourceQuestionId,
        String summary,
        FindingStatus status,
        String proposedBy,
        Instant proposedAt,
        String reviewedBy,
        String reviewRationale,
        Instant reviewedAt,
        List<FindingReviewEventResponse> events) {
    static FindingResponse from(FindingSnapshot finding) {
        return new FindingResponse(
                finding.id(),
                finding.sourceQuestionId(),
                finding.summary(),
                finding.status(),
                finding.proposedBy(),
                finding.proposedAt(),
                finding.reviewedBy(),
                finding.reviewRationale(),
                finding.reviewedAt(),
                finding.events().stream().map(FindingReviewEventResponse::from).toList());
    }
}
