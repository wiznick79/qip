package io.github.wiznick79.qip.investigations.internal.domain;

import io.github.wiznick79.qip.investigations.api.InvestigationStatus;
import io.github.wiznick79.qip.investigations.internal.application.InvalidInvestigationStateException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Investigation(
        UUID id,
        UUID incidentId,
        InvestigationStatus status,
        String closureSummary,
        String closedBy,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt) {
    public Investigation {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(incidentId, "incidentId is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not precede createdAt");
        }
        if (status == InvestigationStatus.OPEN) {
            if (closureSummary != null || closedBy != null || closedAt != null) {
                throw new InvalidInvestigationStateException("An open investigation cannot contain closure details");
            }
        } else {
            closureSummary = bounded(closureSummary, 4_000, "Closure summary");
            closedBy = bounded(closedBy, 120, "Closed-by reference");
            Objects.requireNonNull(closedAt, "closedAt is required");
            if (closedAt.isBefore(createdAt)) {
                throw new InvalidInvestigationStateException("Closure time must not precede creation time");
            }
        }
    }

    public void requireOpen() {
        if (status != InvestigationStatus.OPEN) {
            throw new InvalidInvestigationStateException("The investigation is closed and cannot be changed");
        }
    }

    public Investigation close(String summary, String closerReference, Instant closureTime) {
        requireOpen();
        if (closureTime.isBefore(updatedAt)) {
            throw new InvalidInvestigationStateException("Closure time must not precede the last investigation update");
        }
        return new Investigation(
                id,
                incidentId,
                InvestigationStatus.CLOSED,
                summary,
                closerReference,
                closureTime,
                createdAt,
                closureTime);
    }

    public Investigation touch(Instant changeTime) {
        requireOpen();
        if (changeTime.isBefore(updatedAt)) {
            throw new InvalidInvestigationStateException("Change time must not precede the last investigation update");
        }
        return new Investigation(id, incidentId, status, null, null, null, createdAt, changeTime);
    }

    private static String bounded(String value, int maximum, String field) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new InvalidInvestigationStateException(field + " must contain 1 to " + maximum + " characters");
        }
        return value.trim();
    }
}
