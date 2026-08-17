package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataObservationRepository extends JpaRepository<ObservationJpaEntity, UUID> {

    Page<ObservationJpaEntity> findByIncidentId(UUID incidentId, Pageable pageable);
}
