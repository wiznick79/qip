package io.github.wiznick79.qip.assets.internal.infrastructure.web;

import io.github.wiznick79.qip.assets.api.AssetSnapshot;
import io.github.wiznick79.qip.assets.api.AssetType;
import io.github.wiznick79.qip.assets.internal.domain.Asset;
import java.time.Instant;
import java.util.UUID;

record AssetResponse(UUID id, String name, AssetType type, String externalReference, Instant createdAt) {

    static AssetResponse from(AssetSnapshot asset) {
        return new AssetResponse(asset.id(), asset.name(), asset.type(), asset.externalReference(), asset.createdAt());
    }

    static AssetResponse from(Asset asset) {
        return new AssetResponse(asset.id(), asset.name(), asset.type(), asset.externalReference(), asset.createdAt());
    }
}
