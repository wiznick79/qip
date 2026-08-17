package io.github.wiznick79.qip.assets.internal.application;

import io.github.wiznick79.qip.assets.internal.domain.Asset;
import java.util.List;

public record AssetPage(List<Asset> items, int page, int size, long totalElements) {

    public AssetPage {
        items = List.copyOf(items);
    }
}
