package io.github.wiznick79.qip.incidents.internal.domain;

import java.time.Instant;

public final class InvalidEvidenceTimeException extends RuntimeException {

    private final Instant eventAt;
    private final Instant recordedAt;

    public InvalidEvidenceTimeException(Instant eventAt, Instant recordedAt) {
        super("Evidence event time must not be later than its recording time");
        this.eventAt = eventAt;
        this.recordedAt = recordedAt;
    }

    public Instant eventAt() {
        return eventAt;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}
