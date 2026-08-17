package io.github.wiznick79.qip.assets.internal.application;

import io.github.wiznick79.qip.assets.internal.domain.Asset;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository {

    Asset save(Asset asset);

    Optional<Asset> findById(UUID assetId);

    boolean existsById(UUID assetId);

    AssetPage findAll(int page, int size);
}
