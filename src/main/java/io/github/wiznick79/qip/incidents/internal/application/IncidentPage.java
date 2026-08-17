package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.internal.domain.Incident;
import java.util.List;

public record IncidentPage(List<Incident> items, int page, int size, long totalElements) {

    public IncidentPage {
        items = List.copyOf(items);
    }
}
