package io.github.wiznick79.qip.assets.internal.infrastructure.web;

import java.util.List;

record AssetPageResponse(List<AssetResponse> items, int page, int size, long totalElements) {

    AssetPageResponse {
        items = List.copyOf(items);
    }
}
