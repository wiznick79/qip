package io.github.wiznick79.qip.assets.api;

import java.util.UUID;

public final class AssetNotFoundException extends RuntimeException {

    private final UUID assetId;

    public AssetNotFoundException(UUID assetId) {
        super("Asset not found: " + assetId);
        this.assetId = assetId;
    }

    public UUID assetId() {
        return assetId;
    }
}
