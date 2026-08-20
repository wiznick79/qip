package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.api.IncidentNotFoundException;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceItem;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceProvenance;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceManagement {

    private final IncidentRepository incidents;
    private final EvidenceRepository evidence;
    private final EvidenceIdGenerator idGenerator;
    private final Clock clock;

    public EvidenceManagement(
            IncidentRepository incidents, EvidenceRepository evidence, EvidenceIdGenerator idGenerator, Clock clock) {
        this.incidents = incidents;
        this.evidence = evidence;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public EvidenceItem append(UUID incidentId, AppendEvidenceCommand command) {
        requireIncident(incidentId);
        EvidenceItem item = new EvidenceItem(
                idGenerator.nextId(),
                incidentId,
                command.type(),
                command.summary(),
                command.sourceReference(),
                command.eventAt(),
                EvidenceProvenance.HUMAN_ENTERED,
                command.submittedBy(),
                Instant.now(clock));
        return evidence.save(item);
    }

    @Transactional(readOnly = true)
    public EvidencePage list(UUID incidentId, int page, int size) {
        requireIncident(incidentId);
        return evidence.findByIncidentId(incidentId, page, size);
    }

    private void requireIncident(UUID incidentId) {
        if (!incidents.existsById(incidentId)) {
            throw new IncidentNotFoundException(incidentId);
        }
    }
}
