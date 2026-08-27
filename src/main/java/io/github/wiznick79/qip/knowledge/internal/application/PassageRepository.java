package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PassageRepository {
    void replaceAll(UUID documentId, List<KnowledgePassage> passages);

    List<RetrievedPassage> searchSemantic(Embedding query, String embeddingModel, Set<UUID> documentIds, int limit);

    List<RetrievedPassage> searchLexical(String query, Set<UUID> documentIds, int limit);
}
