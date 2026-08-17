package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import io.github.wiznick79.qip.incidents.internal.domain.EvidenceItem;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceProvenance;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_evidence")
class EvidenceJpaEntity {

    @Id
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 30)
    private EvidenceType type;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(name = "source_reference", nullable = false, length = 500)
    private String sourceReference;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EvidenceProvenance provenance;

    @Column(name = "submitted_by", nullable = false, length = 120)
    private String submittedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected EvidenceJpaEntity() {}

    private EvidenceJpaEntity(EvidenceItem evidence) {
        id = evidence.id();
        incidentId = evidence.incidentId();
        type = evidence.type();
        summary = evidence.summary();
        sourceReference = evidence.sourceReference();
        eventAt = evidence.eventAt();
        provenance = evidence.provenance();
        submittedBy = evidence.submittedBy();
        recordedAt = evidence.recordedAt();
    }

    static EvidenceJpaEntity fromDomain(EvidenceItem evidence) {
        return new EvidenceJpaEntity(evidence);
    }

    EvidenceItem toDomain() {
        return new EvidenceItem(
                id, incidentId, type, summary, sourceReference, eventAt, provenance, submittedBy, recordedAt);
    }
}
