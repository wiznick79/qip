package io.github.wiznick79.qip.incidents.internal.application;

import java.time.Instant;

public final class InvalidIncidentSearchRangeException extends RuntimeException {

    private final Instant from;
    private final Instant to;

    InvalidIncidentSearchRangeException(Instant from, Instant to) {
        super("Incident search 'from' must be before 'to'");
        this.from = from;
        this.to = to;
    }

    public Instant from() {
        return from;
    }

    public Instant to() {
        return to;
    }
}
