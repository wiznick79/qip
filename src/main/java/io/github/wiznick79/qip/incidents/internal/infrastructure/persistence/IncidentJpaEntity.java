package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import io.github.wiznick79.qip.incidents.api.IncidentSeverity;
import io.github.wiznick79.qip.incidents.api.IncidentStatus;
import io.github.wiznick79.qip.incidents.internal.domain.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
class IncidentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IncidentJpaEntity() {}

    private IncidentJpaEntity(Incident incident) {
        id = incident.id();
        assetId = incident.assetId();
        title = incident.title();
        description = incident.description();
        severity = incident.severity();
        status = incident.status();
        occurredAt = incident.occurredAt();
        createdAt = incident.createdAt();
        updatedAt = incident.updatedAt();
    }

    static IncidentJpaEntity fromDomain(Incident incident) {
        return new IncidentJpaEntity(incident);
    }

    Incident toDomain() {
        return new Incident(id, assetId, title, description, severity, status, occurredAt, createdAt, updatedAt);
    }
}
