package io.github.wiznick79.qip.investigations.internal.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Investigation(UUID id, UUID incidentId, Instant createdAt, Instant updatedAt) {
    public Investigation {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(incidentId, "incidentId is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not precede createdAt");
        }
    }
}
