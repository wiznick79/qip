package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface SpringDataIncidentRepository
        extends JpaRepository<IncidentJpaEntity, UUID>, JpaSpecificationExecutor<IncidentJpaEntity> {}
