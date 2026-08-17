package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.internal.domain.Observation;
import java.util.List;

public record ObservationPage(List<Observation> items, int page, int size, long totalElements) {

    public ObservationPage {
        items = List.copyOf(items);
    }
}
