package io.github.wiznick79.qip.assets.internal.application;

import io.github.wiznick79.qip.assets.api.AssetCatalog;
import io.github.wiznick79.qip.assets.api.AssetNotFoundException;
import io.github.wiznick79.qip.assets.api.AssetSnapshot;
import io.github.wiznick79.qip.assets.internal.domain.Asset;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetManagement implements AssetCatalog {

    private final AssetRepository repository;
    private final AssetIdGenerator idGenerator;
    private final Clock clock;

    public AssetManagement(AssetRepository repository, AssetIdGenerator idGenerator, Clock clock) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public AssetSnapshot createAsset(CreateAssetCommand command) {
        Asset asset = new Asset(
                idGenerator.nextId(), command.name(), command.type(), command.externalReference(), Instant.now(clock));
        return snapshot(repository.save(asset));
    }

    @Override
    @Transactional(readOnly = true)
    public AssetSnapshot getAsset(UUID assetId) {
        return repository
                .findById(assetId)
                .map(AssetManagement::snapshot)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean assetExists(UUID assetId) {
        return repository.existsById(assetId);
    }

    @Transactional(readOnly = true)
    public AssetPage listAssets(int page, int size) {
        return repository.findAll(page, size);
    }

    private static AssetSnapshot snapshot(Asset asset) {
        return new AssetSnapshot(asset.id(), asset.name(), asset.type(), asset.externalReference(), asset.createdAt());
    }
}
