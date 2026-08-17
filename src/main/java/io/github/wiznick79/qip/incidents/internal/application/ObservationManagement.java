package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.api.IncidentNotFoundException;
import io.github.wiznick79.qip.incidents.internal.domain.Observation;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObservationManagement {

    private final IncidentRepository incidents;
    private final ObservationRepository observations;
    private final ObservationIdGenerator idGenerator;
    private final Clock clock;

    public ObservationManagement(
            IncidentRepository incidents,
            ObservationRepository observations,
            ObservationIdGenerator idGenerator,
            Clock clock) {
        this.incidents = incidents;
        this.observations = observations;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public Observation append(UUID incidentId, AppendObservationCommand command) {
        requireIncident(incidentId);
        Observation observation = new Observation(
                idGenerator.nextId(),
                incidentId,
                command.text(),
                command.authorReference(),
                command.observedAt(),
                Instant.now(clock));
        return observations.save(observation);
    }

    @Transactional(readOnly = true)
    public ObservationPage list(UUID incidentId, int page, int size) {
        requireIncident(incidentId);
        return observations.findByIncidentId(incidentId, page, size);
    }

    private void requireIncident(UUID incidentId) {
        if (!incidents.existsById(incidentId)) {
            throw new IncidentNotFoundException(incidentId);
        }
    }
}
