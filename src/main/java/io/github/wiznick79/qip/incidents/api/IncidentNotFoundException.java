package io.github.wiznick79.qip.incidents.api;

import java.util.UUID;

public final class IncidentNotFoundException extends RuntimeException {

    private final UUID incidentId;

    public IncidentNotFoundException(UUID incidentId) {
        super("Incident not found: " + incidentId);
        this.incidentId = incidentId;
    }

    public UUID incidentId() {
        return incidentId;
    }
}
