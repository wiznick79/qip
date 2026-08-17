package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.internal.domain.Incident;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository {

    Incident save(Incident incident);

    Optional<Incident> findById(UUID incidentId);

    boolean existsById(UUID incidentId);

    IncidentPage search(IncidentSearchCriteria criteria);
}
