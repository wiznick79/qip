package io.github.wiznick79.qip.assets.internal.application;

import java.util.UUID;

@FunctionalInterface
public interface AssetIdGenerator {

    UUID nextId();
}
