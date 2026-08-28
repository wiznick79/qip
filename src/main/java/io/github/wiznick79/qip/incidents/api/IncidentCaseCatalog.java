package io.github.wiznick79.qip.incidents.api;

import java.util.UUID;

public interface IncidentCaseCatalog {

    IncidentCaseSnapshot getCase(UUID incidentId);
}
