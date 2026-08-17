package io.github.wiznick79.qip.incidents.api;

public final class InvalidIncidentTransitionException extends RuntimeException {

    private final IncidentStatus currentStatus;
    private final IncidentStatus requestedStatus;

    public InvalidIncidentTransitionException(IncidentStatus currentStatus, IncidentStatus requestedStatus) {
        super("Incident cannot transition from " + currentStatus + " to " + requestedStatus);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public IncidentStatus currentStatus() {
        return currentStatus;
    }

    public IncidentStatus requestedStatus() {
        return requestedStatus;
    }
}
