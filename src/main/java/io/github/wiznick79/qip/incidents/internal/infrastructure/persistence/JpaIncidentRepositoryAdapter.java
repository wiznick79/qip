package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import io.github.wiznick79.qip.incidents.internal.application.IncidentPage;
import io.github.wiznick79.qip.incidents.internal.application.IncidentRepository;
import io.github.wiznick79.qip.incidents.internal.application.IncidentSearchCriteria;
import io.github.wiznick79.qip.incidents.internal.domain.Incident;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class JpaIncidentRepositoryAdapter implements IncidentRepository {

    private static final Sort INCIDENT_ORDER =
            Sort.by("occurredAt").descending().and(Sort.by("id").ascending());

    private final SpringDataIncidentRepository repository;

    JpaIncidentRepositoryAdapter(SpringDataIncidentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Incident save(Incident incident) {
        return repository.save(IncidentJpaEntity.fromDomain(incident)).toDomain();
    }

    @Override
    public Optional<Incident> findById(UUID incidentId) {
        return repository.findById(incidentId).map(IncidentJpaEntity::toDomain);
    }

    @Override
    public boolean existsById(UUID incidentId) {
        return repository.existsById(incidentId);
    }

    @Override
    public IncidentPage search(IncidentSearchCriteria criteria) {
        Specification<IncidentJpaEntity> specification = (root, query, builder) -> builder.conjunction();
        if (criteria.assetId() != null) {
            specification =
                    specification.and((root, query, builder) -> builder.equal(root.get("assetId"), criteria.assetId()));
        }
        if (criteria.status() != null) {
            specification =
                    specification.and((root, query, builder) -> builder.equal(root.get("status"), criteria.status()));
        }
        if (criteria.from() != null) {
            specification = specification.and(occurredAtOnOrAfter(criteria.from()));
        }
        if (criteria.to() != null) {
            specification = specification.and(occurredAtBefore(criteria.to()));
        }

        var result =
                repository.findAll(specification, PageRequest.of(criteria.page(), criteria.size(), INCIDENT_ORDER));
        return new IncidentPage(
                result.getContent().stream().map(IncidentJpaEntity::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    private static Specification<IncidentJpaEntity> occurredAtOnOrAfter(Instant from) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    private static Specification<IncidentJpaEntity> occurredAtBefore(Instant to) {
        return (root, query, builder) -> builder.lessThan(root.get("occurredAt"), to);
    }
}
