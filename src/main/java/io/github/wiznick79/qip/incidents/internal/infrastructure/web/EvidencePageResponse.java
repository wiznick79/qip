package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import java.util.List;

record EvidencePageResponse(List<EvidenceResponse> items, int page, int size, long totalElements) {

    EvidencePageResponse {
        items = List.copyOf(items);
    }
}
