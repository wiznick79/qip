package io.github.wiznick79.qip.incidents.api;

import java.time.Instant;
import java.util.UUID;

public record IncidentObservationSnapshot(
        UUID id, String text, String authorReference, Instant observedAt, Instant recordedAt) {}
