package io.github.wiznick79.qip.knowledge.internal.application;

import java.util.List;
import java.util.UUID;

public interface DocumentIndexer {
    void index(UUID documentId, List<ExtractedPage> pages);
}
