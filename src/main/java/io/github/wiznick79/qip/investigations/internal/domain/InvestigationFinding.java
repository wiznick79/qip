package io.github.wiznick79.qip.investigations.internal.domain;

import io.github.wiznick79.qip.investigations.api.FindingStatus;
import io.github.wiznick79.qip.investigations.internal.application.InvalidFindingException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InvestigationFinding(
        UUID id,
        UUID investigationId,
        UUID sourceQuestionId,
        String summary,
        FindingStatus status,
        String proposedBy,
        Instant proposedAt,
        String reviewedBy,
        String reviewRationale,
        Instant reviewedAt) {

    public InvestigationFinding {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(investigationId, "investigationId is required");
        Objects.requireNonNull(sourceQuestionId, "sourceQuestionId is required");
        summary = bounded(summary, 2_000, "summary");
        Objects.requireNonNull(status, "status is required");
        proposedBy = bounded(proposedBy, 120, "proposedBy");
        Objects.requireNonNull(proposedAt, "proposedAt is required");
        if (status == FindingStatus.DRAFT) {
            if (reviewedBy != null || reviewRationale != null || reviewedAt != null) {
                throw new InvalidFindingException("A draft finding cannot contain review details");
            }
        } else {
            reviewedBy = bounded(reviewedBy, 120, "reviewerReference");
            reviewRationale = bounded(reviewRationale, 1_000, "rationale");
            Objects.requireNonNull(reviewedAt, "reviewedAt is required");
            if (reviewedAt.isBefore(proposedAt)) {
                throw new InvalidFindingException("Review time must not precede proposal time");
            }
        }
    }

    public InvestigationFinding review(
            FindingStatus decision, String reviewerReference, String rationale, Instant reviewTime) {
        if (status != FindingStatus.DRAFT) {
            throw new InvalidFindingException("Only draft findings can be reviewed");
        }
        if (decision != FindingStatus.CONFIRMED && decision != FindingStatus.REJECTED) {
            throw new InvalidFindingException("A review decision must be CONFIRMED or REJECTED");
        }
        return new InvestigationFinding(
                id,
                investigationId,
                sourceQuestionId,
                summary,
                decision,
                proposedBy,
                proposedAt,
                reviewerReference,
                rationale,
                reviewTime);
    }

    private static String bounded(String value, int maximum, String field) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new InvalidFindingException(field + " must contain 1 to " + maximum + " characters");
        }
        return value.trim();
    }
}
