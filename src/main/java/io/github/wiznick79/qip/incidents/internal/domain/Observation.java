package io.github.wiznick79.qip.incidents.internal.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Observation(
        UUID id, UUID incidentId, String text, String authorReference, Instant observedAt, Instant recordedAt) {

    private static final int MAX_TEXT_LENGTH = 4000;
    private static final int MAX_AUTHOR_REFERENCE_LENGTH = 120;

    public Observation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(incidentId, "incidentId must not be null");
        text = requiredText(text, "text", MAX_TEXT_LENGTH);
        authorReference = requiredText(authorReference, "authorReference", MAX_AUTHOR_REFERENCE_LENGTH);
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        if (observedAt.isAfter(recordedAt)) {
            throw new InvalidObservationTimeException(observedAt, recordedAt);
        }
    }

    private static String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }
}
