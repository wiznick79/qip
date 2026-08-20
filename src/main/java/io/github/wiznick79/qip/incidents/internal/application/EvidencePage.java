package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.internal.domain.EvidenceItem;
import java.util.List;

public record EvidencePage(List<EvidenceItem> items, int page, int size, long totalElements) {

    public EvidencePage {
        items = List.copyOf(items);
    }
}
