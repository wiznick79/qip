package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.internal.domain.Observation;
import java.util.UUID;

public interface ObservationRepository {

    Observation save(Observation observation);

    ObservationPage findByIncidentId(UUID incidentId, int page, int size);
}
