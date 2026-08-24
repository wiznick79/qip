package io.github.wiznick79.qip.incidents.api;

import java.util.UUID;

public interface IncidentCatalog {

    IncidentSnapshot getIncident(UUID incidentId);

    boolean incidentExists(UUID incidentId);

    IncidentSnapshot markInvestigationStarted(UUID incidentId);

    IncidentSnapshot markInvestigationCompleted(UUID incidentId);
}
