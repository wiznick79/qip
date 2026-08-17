package io.github.wiznick79.qip.incidents.api;

import java.time.Instant;
import java.util.UUID;

public record IncidentSnapshot(
        UUID id,
        UUID assetId,
        String title,
        String description,
        IncidentSeverity severity,
        IncidentStatus status,
        Instant occurredAt,
        Instant createdAt,
        Instant updatedAt) {}
