package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import io.github.wiznick79.qip.incidents.internal.application.ObservationPage;
import io.github.wiznick79.qip.incidents.internal.application.ObservationRepository;
import io.github.wiznick79.qip.incidents.internal.domain.Observation;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaObservationRepositoryAdapter implements ObservationRepository {

    private static final Sort OBSERVATION_ORDER =
            Sort.by("observedAt").ascending().and(Sort.by("id").ascending());

    private final SpringDataObservationRepository repository;

    JpaObservationRepositoryAdapter(SpringDataObservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Observation save(Observation observation) {
        return repository.save(ObservationJpaEntity.fromDomain(observation)).toDomain();
    }

    @Override
    public ObservationPage findByIncidentId(UUID incidentId, int page, int size) {
        var result = repository.findByIncidentId(incidentId, PageRequest.of(page, size, OBSERVATION_ORDER));
        return new ObservationPage(
                result.getContent().stream().map(ObservationJpaEntity::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }
}
