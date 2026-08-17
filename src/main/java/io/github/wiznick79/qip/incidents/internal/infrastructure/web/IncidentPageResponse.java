package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import java.util.List;

record IncidentPageResponse(List<IncidentResponse> items, int page, int size, long totalElements) {

    IncidentPageResponse {
        items = List.copyOf(items);
    }
}
