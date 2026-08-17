package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import io.github.wiznick79.qip.incidents.internal.domain.Observation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_observations")
class ObservationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "observation_text", nullable = false, length = 4000)
    private String text;

    @Column(name = "author_reference", nullable = false, length = 120)
    private String authorReference;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected ObservationJpaEntity() {}

    private ObservationJpaEntity(Observation observation) {
        id = observation.id();
        incidentId = observation.incidentId();
        text = observation.text();
        authorReference = observation.authorReference();
        observedAt = observation.observedAt();
        recordedAt = observation.recordedAt();
    }

    static ObservationJpaEntity fromDomain(Observation observation) {
        return new ObservationJpaEntity(observation);
    }

    Observation toDomain() {
        return new Observation(id, incidentId, text, authorReference, observedAt, recordedAt);
    }
}
