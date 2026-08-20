package io.github.wiznick79.qip.incidents.internal.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EvidenceItem(
        UUID id,
        UUID incidentId,
        EvidenceType type,
        String summary,
        String sourceReference,
        Instant eventAt,
        EvidenceProvenance provenance,
        String submittedBy,
        Instant recordedAt) {

    private static final int MAX_SUMMARY_LENGTH = 1000;
    private static final int MAX_SOURCE_REFERENCE_LENGTH = 500;
    private static final int MAX_SUBMITTED_BY_LENGTH = 120;

    public EvidenceItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(incidentId, "incidentId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        summary = requiredText(summary, "summary", MAX_SUMMARY_LENGTH);
        sourceReference = requiredText(sourceReference, "sourceReference", MAX_SOURCE_REFERENCE_LENGTH);
        Objects.requireNonNull(eventAt, "eventAt must not be null");
        Objects.requireNonNull(provenance, "provenance must not be null");
        submittedBy = requiredText(submittedBy, "submittedBy", MAX_SUBMITTED_BY_LENGTH);
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        if (eventAt.isAfter(recordedAt)) {
            throw new InvalidEvidenceTimeException(eventAt, recordedAt);
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
