package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.internal.domain.EvidenceItem;
import java.util.UUID;

public interface EvidenceRepository {

    EvidenceItem save(EvidenceItem evidence);

    EvidencePage findByIncidentId(UUID incidentId, int page, int size);
}
