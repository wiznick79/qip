package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import io.github.wiznick79.qip.incidents.internal.domain.Observation;
import java.time.Instant;
import java.util.UUID;

record ObservationResponse(
        UUID id, UUID incidentId, String text, String authorReference, Instant observedAt, Instant recordedAt) {

    static ObservationResponse from(Observation observation) {
        return new ObservationResponse(
                observation.id(),
                observation.incidentId(),
                observation.text(),
                observation.authorReference(),
                observation.observedAt(),
                observation.recordedAt());
    }
}
