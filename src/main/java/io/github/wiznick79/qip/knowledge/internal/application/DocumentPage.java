package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.api.DocumentSnapshot;
import java.util.List;

public record DocumentPage(List<DocumentSnapshot> items, int page, int size, long totalElements) {
    public DocumentPage {
        items = List.copyOf(items);
    }
}
