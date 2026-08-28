package io.github.wiznick79.qip.incidents.api;

import java.time.Instant;
import java.util.UUID;

public record IncidentEvidenceSnapshot(
        UUID id,
        String type,
        String summary,
        String sourceReference,
        Instant eventAt,
        String provenance,
        String submittedBy,
        Instant recordedAt) {}
