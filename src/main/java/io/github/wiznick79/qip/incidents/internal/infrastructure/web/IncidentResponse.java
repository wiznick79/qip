package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import io.github.wiznick79.qip.incidents.api.IncidentSeverity;
import io.github.wiznick79.qip.incidents.api.IncidentSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentStatus;
import io.github.wiznick79.qip.incidents.internal.domain.Incident;
import java.time.Instant;
import java.util.UUID;

record IncidentResponse(
        UUID id,
        UUID assetId,
        String title,
        String description,
        IncidentSeverity severity,
        IncidentStatus status,
        Instant occurredAt,
        Instant createdAt,
        Instant updatedAt) {

    static IncidentResponse from(IncidentSnapshot incident) {
        return new IncidentResponse(
                incident.id(),
                incident.assetId(),
                incident.title(),
                incident.description(),
                incident.severity(),
                incident.status(),
                incident.occurredAt(),
                incident.createdAt(),
                incident.updatedAt());
    }

    static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.id(),
                incident.assetId(),
                incident.title(),
                incident.description(),
                incident.severity(),
                incident.status(),
                incident.occurredAt(),
                incident.createdAt(),
                incident.updatedAt());
    }
}
