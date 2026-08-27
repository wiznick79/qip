package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.api.KnowledgeQuery;
import io.github.wiznick79.qip.knowledge.api.KnowledgeSearch;
import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class KnowledgeSearchService implements KnowledgeSearch {

    private static final int CANDIDATE_MULTIPLIER = 3;
    private static final int MAX_CANDIDATES_PER_RANKING = 60;

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
            int candidateLimit = Math.min(MAX_CANDIDATES_PER_RANKING, query.limit() * CANDIDATE_MULTIPLIER);
            List<RetrievedPassage> semantic = passages.searchSemantic(
                    vectors.getFirst(), embeddings.modelId(), query.documentIds(), candidateLimit);
            List<RetrievedPassage> lexical = passages.searchLexical(query.text(), query.documentIds(), candidateLimit);
            List<RetrievedPassage> result = ReciprocalRankFusion.fuse(semantic, lexical, query.limit());
            metrics.recordRetrieval(sample, "success");
            return result;
        } catch (RuntimeException exception) {
            metrics.recordRetrieval(sample, "failure");
            throw exception;
        }
    }
}
