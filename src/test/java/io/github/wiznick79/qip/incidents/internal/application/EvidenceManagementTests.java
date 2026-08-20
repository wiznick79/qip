package io.github.wiznick79.qip.incidents.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.incidents.api.IncidentNotFoundException;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceItem;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceProvenance;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceType;
import io.github.wiznick79.qip.incidents.internal.domain.Incident;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceManagementTests {

    private static final UUID EVIDENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000801");
    private static final UUID INCIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000802");
    private static final UUID MISSING_INCIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000899");
    private static final Instant NOW = Instant.parse("2026-08-17T17:00:00Z");
    private static final Instant EVENT_AT = Instant.parse("2026-08-17T16:30:00Z");

    private final ExistingIncidentRepository incidents = new ExistingIncidentRepository();
    private final InMemoryEvidenceRepository evidence = new InMemoryEvidenceRepository();
    private final EvidenceManagement management =
            new EvidenceManagement(incidents, evidence, () -> EVIDENCE_ID, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void appendsSourceAttributedEvidenceWithServerOwnedHumanProvenance() {
        EvidenceItem created = management.append(
                INCIDENT_ID,
                new AppendEvidenceCommand(
                        EvidenceType.MEASUREMENT,
                        "Pressure measured at 0 bar.",
                        "sensor:pressure-gauge-07",
                        EVENT_AT,
                        "investigator-17"));

        assertThat(created.id()).isEqualTo(EVIDENCE_ID);
        assertThat(created.incidentId()).isEqualTo(INCIDENT_ID);
        assertThat(created.provenance()).isEqualTo(EvidenceProvenance.HUMAN_ENTERED);
        assertThat(created.recordedAt()).isEqualTo(NOW);
        assertThat(evidence.saved).containsExactly(created);
    }

    @Test
    void rejectsAppendForMissingIncidentBeforePersistence() {
        var command = new AppendEvidenceCommand(
                EvidenceType.MEASUREMENT,
                "Pressure measured at 0 bar.",
                "sensor:pressure-gauge-07",
                EVENT_AT,
                "investigator-17");

        assertThatThrownBy(() -> management.append(MISSING_INCIDENT_ID, command))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessage("Incident not found: " + MISSING_INCIDENT_ID);
        assertThat(evidence.saved).isEmpty();
    }

    @Test
    void rejectsListingForMissingIncident() {
        assertThatThrownBy(() -> management.list(MISSING_INCIDENT_ID, 0, 20))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    private static final class ExistingIncidentRepository implements IncidentRepository {

        @Override
        public Incident save(Incident incident) {
            throw new UnsupportedOperationException("Not needed by these tests");
        }

        @Override
        public Optional<Incident> findById(UUID incidentId) {
            throw new UnsupportedOperationException("Not needed by these tests");
        }

        @Override
        public boolean existsById(UUID incidentId) {
            return INCIDENT_ID.equals(incidentId);
        }

        @Override
        public IncidentPage search(IncidentSearchCriteria criteria) {
            throw new UnsupportedOperationException("Not needed by these tests");
        }
    }

    private static final class InMemoryEvidenceRepository implements EvidenceRepository {

        private final List<EvidenceItem> saved = new ArrayList<>();

        @Override
        public EvidenceItem save(EvidenceItem item) {
            saved.add(item);
            return item;
        }

        @Override
        public EvidencePage findByIncidentId(UUID incidentId, int page, int size) {
            return new EvidencePage(saved, page, size, saved.size());
        }
    }
}
