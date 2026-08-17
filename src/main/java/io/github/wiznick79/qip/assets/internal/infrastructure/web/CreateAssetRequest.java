package io.github.wiznick79.qip.assets.internal.infrastructure.web;

import io.github.wiznick79.qip.assets.api.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record CreateAssetRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull AssetType type,
        @Size(max = 100) String externalReference) {}
