package io.github.wiznick79.qip.incidents.api;

import java.util.List;

public record IncidentCaseSnapshot(
        IncidentSnapshot incident,
        List<IncidentObservationSnapshot> observations,
        List<IncidentEvidenceSnapshot> evidence) {

    public IncidentCaseSnapshot {
        observations = List.copyOf(observations);
        evidence = List.copyOf(evidence);
    }
}
