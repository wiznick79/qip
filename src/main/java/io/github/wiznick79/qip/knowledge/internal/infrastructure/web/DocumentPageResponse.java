package io.github.wiznick79.qip.knowledge.internal.infrastructure.web;

import io.github.wiznick79.qip.knowledge.internal.application.DocumentPage;
import java.util.List;

record DocumentPageResponse(List<DocumentResponse> items, int page, int size, long totalElements) {
    DocumentPageResponse {
        items = List.copyOf(items);
    }

    static DocumentPageResponse from(DocumentPage page) {
        return new DocumentPageResponse(
                page.items().stream().map(DocumentResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements());
    }
}
