package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.assets.api.AssetCatalog;
import io.github.wiznick79.qip.assets.api.AssetNotFoundException;
import io.github.wiznick79.qip.incidents.api.IncidentCatalog;
import io.github.wiznick79.qip.incidents.api.IncidentNotFoundException;
import io.github.wiznick79.qip.incidents.api.IncidentSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentStatus;
import io.github.wiznick79.qip.incidents.internal.domain.Incident;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentManagement implements IncidentCatalog {

    private final IncidentRepository repository;
    private final AssetCatalog assets;
    private final IncidentIdGenerator idGenerator;
    private final Clock clock;

    public IncidentManagement(
            IncidentRepository repository, AssetCatalog assets, IncidentIdGenerator idGenerator, Clock clock) {
        this.repository = repository;
        this.assets = assets;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public IncidentSnapshot createIncident(CreateIncidentCommand command) {
        if (!assets.assetExists(command.assetId())) {
            throw new AssetNotFoundException(command.assetId());
        }
        Instant now = Instant.now(clock);
        Incident incident = new Incident(
                idGenerator.nextId(),
                command.assetId(),
                command.title(),
                command.description(),
                command.severity(),
                IncidentStatus.REPORTED,
                command.occurredAt(),
                now,
                now);
        return snapshot(repository.save(incident));
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentSnapshot getIncident(UUID incidentId) {
        return snapshot(findIncident(incidentId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean incidentExists(UUID incidentId) {
        return repository.existsById(incidentId);
    }

    @Override
    @Transactional
    public IncidentSnapshot markInvestigationStarted(UUID incidentId) {
        Incident incident = findIncident(incidentId);
        if (incident.status() != IncidentStatus.REPORTED) {
            return snapshot(incident);
        }
        return snapshot(repository.save(incident.transitionTo(IncidentStatus.INVESTIGATING, Instant.now(clock))));
    }

    @Override
    @Transactional
    public IncidentSnapshot markInvestigationCompleted(UUID incidentId) {
        Incident incident = findIncident(incidentId);
        if (incident.status() == IncidentStatus.CLOSED || incident.status() == IncidentStatus.RESOLVED) {
            return snapshot(incident);
        }
        Instant now = Instant.now(clock);
        if (incident.status() == IncidentStatus.REPORTED) {
            incident = incident.transitionTo(IncidentStatus.INVESTIGATING, now);
        }
        return snapshot(repository.save(incident.transitionTo(IncidentStatus.RESOLVED, now)));
    }

    @Transactional
    public IncidentSnapshot updateStatus(UUID incidentId, IncidentStatus requestedStatus) {
        Incident updated = findIncident(incidentId).transitionTo(requestedStatus, Instant.now(clock));
        return snapshot(repository.save(updated));
    }

    @Transactional(readOnly = true)
    public IncidentPage search(IncidentSearchCriteria criteria) {
        return repository.search(criteria);
    }

    private Incident findIncident(UUID incidentId) {
        return repository.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));
    }

    private static IncidentSnapshot snapshot(Incident incident) {
        return new IncidentSnapshot(
                incident.id(),
                incident.assetId(),
                incident.title(),
                incident.description(),
                incident.severity(),
                incident.status(),
                incident.occurredAt(),
                incident.createdAt(),
                incident.updatedAt());
    }
}
