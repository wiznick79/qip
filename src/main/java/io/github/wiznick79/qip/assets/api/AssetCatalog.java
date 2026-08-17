package io.github.wiznick79.qip.assets.api;

import java.util.UUID;

public interface AssetCatalog {

    AssetSnapshot getAsset(UUID assetId);

    boolean assetExists(UUID assetId);
}
