package io.github.wiznick79.qip.incidents.internal.domain;

import java.time.Instant;

public final class InvalidObservationTimeException extends RuntimeException {

    private final Instant observedAt;
    private final Instant recordedAt;

    public InvalidObservationTimeException(Instant observedAt, Instant recordedAt) {
        super("Observation time must not be later than its recording time");
        this.observedAt = observedAt;
        this.recordedAt = recordedAt;
    }

    public Instant observedAt() {
        return observedAt;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}
