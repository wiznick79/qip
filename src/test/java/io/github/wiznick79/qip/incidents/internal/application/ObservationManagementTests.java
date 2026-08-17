package io.github.wiznick79.qip.incidents.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.incidents.api.IncidentNotFoundException;
import io.github.wiznick79.qip.incidents.internal.domain.Incident;
import io.github.wiznick79.qip.incidents.internal.domain.Observation;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObservationManagementTests {

    private static final UUID OBSERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID INCIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000602");
    private static final UUID MISSING_INCIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000699");
    private static final Instant NOW = Instant.parse("2026-08-17T15:00:00Z");
    private static final Instant OBSERVED_AT = Instant.parse("2026-08-17T14:30:00Z");

    private final ExistingIncidentRepository incidents = new ExistingIncidentRepository();
    private final InMemoryObservationRepository observations = new InMemoryObservationRepository();
    private final ObservationManagement management =
            new ObservationManagement(incidents, observations, () -> OBSERVATION_ID, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void appendsAttributedObservationWithApplicationOwnedIdentityAndTime() {
        Observation created = management.append(
                INCIDENT_ID, new AppendObservationCommand("Pressure gauge read zero.", "investigator-17", OBSERVED_AT));

        assertThat(created.id()).isEqualTo(OBSERVATION_ID);
        assertThat(created.incidentId()).isEqualTo(INCIDENT_ID);
        assertThat(created.authorReference()).isEqualTo("investigator-17");
        assertThat(created.recordedAt()).isEqualTo(NOW);
        assertThat(observations.saved).containsExactly(created);
    }

    @Test
    void rejectsAppendForMissingIncidentBeforePersistence() {
        var command = new AppendObservationCommand("Pressure gauge read zero.", "investigator-17", OBSERVED_AT);

        assertThatThrownBy(() -> management.append(MISSING_INCIDENT_ID, command))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessage("Incident not found: " + MISSING_INCIDENT_ID);
        assertThat(observations.saved).isEmpty();
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

    private static final class InMemoryObservationRepository implements ObservationRepository {

        private final List<Observation> saved = new ArrayList<>();

        @Override
        public Observation save(Observation observation) {
            saved.add(observation);
            return observation;
        }

        @Override
        public ObservationPage findByIncidentId(UUID incidentId, int page, int size) {
            return new ObservationPage(saved, page, size, saved.size());
        }
    }
}
