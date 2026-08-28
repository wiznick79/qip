package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.api.IncidentCaseCatalog;
import io.github.wiznick79.qip.incidents.api.IncidentCaseSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentCatalog;
import io.github.wiznick79.qip.incidents.api.IncidentEvidenceSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentObservationSnapshot;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceItem;
import io.github.wiznick79.qip.incidents.internal.domain.Observation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IncidentCaseManagement implements IncidentCaseCatalog {

    private static final int PAGE_SIZE = 100;

    private final IncidentCatalog incidents;
    private final ObservationRepository observations;
    private final EvidenceRepository evidence;

    IncidentCaseManagement(IncidentCatalog incidents, ObservationRepository observations, EvidenceRepository evidence) {
        this.incidents = incidents;
        this.observations = observations;
        this.evidence = evidence;
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentCaseSnapshot getCase(UUID incidentId) {
        var incident = incidents.getIncident(incidentId);
        return new IncidentCaseSnapshot(
                incident,
                allObservations(incidentId).stream()
                        .map(IncidentCaseManagement::snapshot)
                        .toList(),
                allEvidence(incidentId).stream()
                        .map(IncidentCaseManagement::snapshot)
                        .toList());
    }

    private List<Observation> allObservations(UUID incidentId) {
        List<Observation> result = new ArrayList<>();
        for (int page = 0; ; page++) {
            ObservationPage current = observations.findByIncidentId(incidentId, page, PAGE_SIZE);
            result.addAll(current.items());
            if (result.size() >= current.totalElements()) {
                return result;
            }
        }
    }

    private List<EvidenceItem> allEvidence(UUID incidentId) {
        List<EvidenceItem> result = new ArrayList<>();
        for (int page = 0; ; page++) {
            EvidencePage current = evidence.findByIncidentId(incidentId, page, PAGE_SIZE);
            result.addAll(current.items());
            if (result.size() >= current.totalElements()) {
                return result;
            }
        }
    }

    private static IncidentObservationSnapshot snapshot(Observation observation) {
        return new IncidentObservationSnapshot(
                observation.id(),
                observation.text(),
                observation.authorReference(),
                observation.observedAt(),
                observation.recordedAt());
    }

    private static IncidentEvidenceSnapshot snapshot(EvidenceItem item) {
        return new IncidentEvidenceSnapshot(
                item.id(),
                item.type().name(),
                item.summary(),
                item.sourceReference(),
                item.eventAt(),
                item.provenance().name(),
                item.submittedBy(),
                item.recordedAt());
    }
}
