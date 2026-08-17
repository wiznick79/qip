package io.github.wiznick79.qip.assets.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.assets.api.AssetNotFoundException;
import io.github.wiznick79.qip.assets.api.AssetType;
import io.github.wiznick79.qip.assets.internal.domain.Asset;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssetManagementTests {

    private static final UUID ASSET_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final Instant NOW = Instant.parse("2026-08-17T13:00:00Z");

    private final InMemoryAssetRepository repository = new InMemoryAssetRepository();
    private final AssetManagement assets =
            new AssetManagement(repository, () -> ASSET_ID, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsAssetWithApplicationOwnedIdentityAndTime() {
        var created = assets.createAsset(new CreateAssetCommand("Mixer 02", AssetType.MACHINE, "MIX-02"));

        assertThat(created.id()).isEqualTo(ASSET_ID);
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(assets.getAsset(ASSET_ID)).isEqualTo(created);
    }

    @Test
    void reportsMissingAssetThroughPublicModuleException() {
        UUID missingId = UUID.fromString("00000000-0000-0000-0000-000000000299");

        assertThatThrownBy(() -> assets.getAsset(missingId))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found: " + missingId);
    }

    private static final class InMemoryAssetRepository implements AssetRepository {

        private final Map<UUID, Asset> assets = new LinkedHashMap<>();

        @Override
        public Asset save(Asset asset) {
            assets.put(asset.id(), asset);
            return asset;
        }

        @Override
        public Optional<Asset> findById(UUID assetId) {
            return Optional.ofNullable(assets.get(assetId));
        }

        @Override
        public boolean existsById(UUID assetId) {
            return assets.containsKey(assetId);
        }

        @Override
        public AssetPage findAll(int page, int size) {
            throw new UnsupportedOperationException("Not needed by these tests");
        }
    }
}
