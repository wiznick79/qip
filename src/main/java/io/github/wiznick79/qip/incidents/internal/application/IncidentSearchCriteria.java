package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.api.IncidentStatus;
import java.time.Instant;
import java.util.UUID;

public record IncidentSearchCriteria(
        UUID assetId, IncidentStatus status, Instant from, Instant to, int page, int size) {

    public IncidentSearchCriteria {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new InvalidIncidentSearchRangeException(from, to);
        }
    }
}
