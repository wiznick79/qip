package io.github.wiznick79.qip.incidents.internal.domain;

import io.github.wiznick79.qip.incidents.api.IncidentSeverity;
import io.github.wiznick79.qip.incidents.api.IncidentStatus;
import io.github.wiznick79.qip.incidents.api.InvalidIncidentTransitionException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Incident(
        UUID id,
        UUID assetId,
        String title,
        String description,
        IncidentSeverity severity,
        IncidentStatus status,
        Instant occurredAt,
        Instant createdAt,
        Instant updatedAt) {

    private static final int MAX_TITLE_LENGTH = 160;
    private static final int MAX_DESCRIPTION_LENGTH = 4000;

    public Incident {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(assetId, "assetId must not be null");
        title = requiredText(title, "title", MAX_TITLE_LENGTH);
        description = optionalText(description, "description", MAX_DESCRIPTION_LENGTH);
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    public Incident transitionTo(IncidentStatus requestedStatus, Instant changedAt) {
        Objects.requireNonNull(requestedStatus, "requestedStatus must not be null");
        Objects.requireNonNull(changedAt, "changedAt must not be null");
        if (requestedStatus == status) {
            return this;
        }
        if (!canTransitionTo(requestedStatus)) {
            throw new InvalidIncidentTransitionException(status, requestedStatus);
        }
        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException("changedAt must not be before updatedAt");
        }
        return new Incident(
                id, assetId, title, description, severity, requestedStatus, occurredAt, createdAt, changedAt);
    }

    private boolean canTransitionTo(IncidentStatus requestedStatus) {
        return switch (status) {
            case REPORTED -> requestedStatus == IncidentStatus.INVESTIGATING;
            case INVESTIGATING -> requestedStatus == IncidentStatus.RESOLVED;
            case RESOLVED ->
                requestedStatus == IncidentStatus.INVESTIGATING || requestedStatus == IncidentStatus.CLOSED;
            case CLOSED -> false;
        };
    }

    private static String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checkedLength(value.trim(), field, maxLength);
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return checkedLength(value.trim(), field, maxLength);
    }

    private static String checkedLength(String value, String field, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
        }
        return value;
    }
}
