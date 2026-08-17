package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import io.github.wiznick79.qip.incidents.internal.application.EvidencePage;
import io.github.wiznick79.qip.incidents.internal.application.EvidenceRepository;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceItem;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaEvidenceRepositoryAdapter implements EvidenceRepository {

    private static final Sort EVIDENCE_ORDER =
            Sort.by("eventAt").ascending().and(Sort.by("id").ascending());

    private final SpringDataEvidenceRepository repository;

    JpaEvidenceRepositoryAdapter(SpringDataEvidenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public EvidenceItem save(EvidenceItem evidence) {
        return repository.save(EvidenceJpaEntity.fromDomain(evidence)).toDomain();
    }

    @Override
    public EvidencePage findByIncidentId(UUID incidentId, int page, int size) {
        var result = repository.findByIncidentId(incidentId, PageRequest.of(page, size, EVIDENCE_ORDER));
        return new EvidencePage(
                result.getContent().stream().map(EvidenceJpaEntity::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }
}
