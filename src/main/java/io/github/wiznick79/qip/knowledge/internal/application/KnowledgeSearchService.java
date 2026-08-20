package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.api.KnowledgeQuery;
import io.github.wiznick79.qip.knowledge.api.KnowledgeSearch;
import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class KnowledgeSearchService implements KnowledgeSearch {

    private final EmbeddingGenerator embeddings;
    private final PassageRepository passages;

    KnowledgeSearchService(EmbeddingGenerator embeddings, PassageRepository passages) {
        this.embeddings = embeddings;
        this.passages = passages;
    }

    @Override
    public List<RetrievedPassage> search(KnowledgeQuery query) {
        List<Embedding> vectors = embeddings.embed(List.of(query.text()));
        if (vectors.size() != 1) {
            throw new DocumentIndexingException("Embedding model returned an unexpected result count");
        }
        return passages.search(vectors.getFirst(), embeddings.modelId(), query.documentIds(), query.limit());
    }
}
