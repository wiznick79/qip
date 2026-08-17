package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import io.github.wiznick79.qip.incidents.api.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

record CreateIncidentRequest(
        @NotNull UUID assetId,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 4000) String description,
        @NotNull IncidentSeverity severity,
        @NotNull Instant occurredAt) {}
