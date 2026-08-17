package io.github.wiznick79.qip.incidents.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.assets.api.AssetCatalog;
import io.github.wiznick79.qip.assets.api.AssetNotFoundException;
import io.github.wiznick79.qip.assets.api.AssetSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentSeverity;
import io.github.wiznick79.qip.incidents.api.IncidentStatus;
import io.github.wiznick79.qip.incidents.api.InvalidIncidentTransitionException;
import io.github.wiznick79.qip.incidents.internal.domain.Incident;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentManagementTests {

    private static final UUID INCIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID ASSET_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");
    private static final UUID MISSING_ASSET_ID = UUID.fromString("00000000-0000-0000-0000-000000000499");
    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");
    private static final Instant OCCURRED_AT = Instant.parse("2026-08-17T08:30:00Z");

    private final InMemoryIncidentRepository repository = new InMemoryIncidentRepository();
    private final IncidentManagement incidents = new IncidentManagement(
            repository, new ExistingAssetCatalog(), () -> INCIDENT_ID, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsReportedIncidentForExistingAsset() {
        var created = incidents.createIncident(
                new CreateIncidentCommand(ASSET_ID, "Coolant leak", null, IncidentSeverity.MEDIUM, OCCURRED_AT));

        assertThat(created.id()).isEqualTo(INCIDENT_ID);
        assertThat(created.assetId()).isEqualTo(ASSET_ID);
        assertThat(created.status()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(incidents.getIncident(INCIDENT_ID)).isEqualTo(created);
    }

    @Test
    void rejectsIncidentForMissingAssetBeforePersistence() {
        var command =
                new CreateIncidentCommand(MISSING_ASSET_ID, "Coolant leak", null, IncidentSeverity.MEDIUM, OCCURRED_AT);

        assertThatThrownBy(() -> incidents.createIncident(command))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found: " + MISSING_ASSET_ID);
        assertThat(repository.incidents).isEmpty();
    }

    @Test
    void appliesLifecycleRulesWhenUpdatingStatus() {
        incidents.createIncident(
                new CreateIncidentCommand(ASSET_ID, "Coolant leak", null, IncidentSeverity.MEDIUM, OCCURRED_AT));

        var investigating = incidents.updateStatus(INCIDENT_ID, IncidentStatus.INVESTIGATING);

        assertThat(investigating.status()).isEqualTo(IncidentStatus.INVESTIGATING);
        assertThatThrownBy(() -> incidents.updateStatus(INCIDENT_ID, IncidentStatus.CLOSED))
                .isInstanceOf(InvalidIncidentTransitionException.class);
    }

    private static final class ExistingAssetCatalog implements AssetCatalog {

        @Override
        public AssetSnapshot getAsset(UUID assetId) {
            throw new UnsupportedOperationException("Not needed by these tests");
        }

        @Override
        public boolean assetExists(UUID assetId) {
            return ASSET_ID.equals(assetId);
        }
    }

    private static final class InMemoryIncidentRepository implements IncidentRepository {

        private final Map<UUID, Incident> incidents = new LinkedHashMap<>();

        @Override
        public Incident save(Incident incident) {
            incidents.put(incident.id(), incident);
            return incident;
        }

        @Override
        public Optional<Incident> findById(UUID incidentId) {
            return Optional.ofNullable(incidents.get(incidentId));
        }

        @Override
        public boolean existsById(UUID incidentId) {
            return incidents.containsKey(incidentId);
        }

        @Override
        public IncidentPage search(IncidentSearchCriteria criteria) {
            throw new UnsupportedOperationException("Not needed by these tests");
        }
    }
}
