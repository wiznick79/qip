package io.github.wiznick79.qip.assets.api;

import java.time.Instant;
import java.util.UUID;

public record AssetSnapshot(UUID id, String name, AssetType type, String externalReference, Instant createdAt) {}
