package io.github.wiznick79.qip.assets.internal.infrastructure.persistence;

import io.github.wiznick79.qip.assets.internal.application.AssetIdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RandomAssetIdGenerator implements AssetIdGenerator {

    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
