package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import io.github.wiznick79.qip.incidents.internal.domain.EvidenceItem;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceProvenance;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceType;
import java.time.Instant;
import java.util.UUID;

record EvidenceResponse(
        UUID id,
        UUID incidentId,
        EvidenceType type,
        String summary,
        String sourceReference,
        Instant eventAt,
        EvidenceProvenance provenance,
        String submittedBy,
        Instant recordedAt) {

    static EvidenceResponse from(EvidenceItem evidence) {
        return new EvidenceResponse(
                evidence.id(),
                evidence.incidentId(),
                evidence.type(),
                evidence.summary(),
                evidence.sourceReference(),
                evidence.eventAt(),
                evidence.provenance(),
                evidence.submittedBy(),
                evidence.recordedAt());
    }
}
