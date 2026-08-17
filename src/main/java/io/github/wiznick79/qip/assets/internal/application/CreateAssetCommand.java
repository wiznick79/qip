package io.github.wiznick79.qip.assets.internal.application;

import io.github.wiznick79.qip.assets.api.AssetType;

public record CreateAssetCommand(String name, AssetType type, String externalReference) {}
