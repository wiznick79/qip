package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.api.IncidentSeverity;
import java.time.Instant;
import java.util.UUID;

public record CreateIncidentCommand(
        UUID assetId, String title, String description, IncidentSeverity severity, Instant occurredAt) {}
