package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import java.util.List;

record ObservationPageResponse(List<ObservationResponse> items, int page, int size, long totalElements) {

    ObservationPageResponse {
        items = List.copyOf(items);
    }
}
