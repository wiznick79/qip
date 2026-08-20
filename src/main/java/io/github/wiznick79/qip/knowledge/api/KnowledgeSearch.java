package io.github.wiznick79.qip.knowledge.api;

import java.util.List;

public interface KnowledgeSearch {
    List<RetrievedPassage> search(KnowledgeQuery query);
}
