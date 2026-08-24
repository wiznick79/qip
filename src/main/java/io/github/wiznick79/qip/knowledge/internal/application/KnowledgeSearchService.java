package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.api.KnowledgeQuery;
import io.github.wiznick79.qip.knowledge.api.KnowledgeSearch;
import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class KnowledgeSearchService implements KnowledgeSearch {

    private final EmbeddingGenerator embeddings;
    private final PassageRepository passages;
    private final KnowledgeOperationalMetrics metrics;

    KnowledgeSearchService(
            EmbeddingGenerator embeddings, PassageRepository passages, KnowledgeOperationalMetrics metrics) {
        this.embeddings = embeddings;
        this.passages = passages;
        this.metrics = metrics;
    }

    @Override
    public List<RetrievedPassage> search(KnowledgeQuery query) {
        Timer.Sample sample = metrics.start();
        try {
            List<Embedding> vectors = embeddings.embed(List.of(query.text()));
            if (vectors.size() != 1) {
                throw new DocumentIndexingException("Embedding model returned an unexpected result count");
            }
            List<RetrievedPassage> result =
                    passages.search(vectors.getFirst(), embeddings.modelId(), query.documentIds(), query.limit());
            metrics.recordRetrieval(sample, "success");
            return result;
        } catch (RuntimeException exception) {
            metrics.recordRetrieval(sample, "failure");
            throw exception;
        }
    }
}
