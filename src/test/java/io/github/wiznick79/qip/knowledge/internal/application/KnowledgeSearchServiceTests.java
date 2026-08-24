package io.github.wiznick79.qip.knowledge.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.wiznick79.qip.knowledge.api.KnowledgeQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class KnowledgeSearchServiceTests {

    @Test
    void recordsBoundedSuccessAndFailureOutcomes() {
        var registry = new SimpleMeterRegistry();
        EmbeddingGenerator embeddings = mock(EmbeddingGenerator.class);
        PassageRepository passages = mock(PassageRepository.class);
        when(embeddings.embed(any())).thenReturn(List.of(new Embedding(List.of(1.0F))));
        when(embeddings.modelId()).thenReturn("synthetic-embedding");
        when(passages.search(any(), anyString(), any(), anyInt()))
                .thenReturn(List.of())
                .thenThrow(new IllegalStateException("synthetic database failure"));
        var search = new KnowledgeSearchService(embeddings, passages, new KnowledgeOperationalMetrics(registry));
        var query = new KnowledgeQuery("synthetic query", Set.of(), 3);

        assertThat(search.search(query)).isEmpty();
        assertThatThrownBy(() -> search.search(query)).isInstanceOf(IllegalStateException.class);

        assertThat(registry.get(KnowledgeOperationalMetrics.RETRIEVAL)
                        .tag("outcome", "success")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(registry.get(KnowledgeOperationalMetrics.RETRIEVAL)
                        .tag("outcome", "failure")
                        .timer()
                        .count())
                .isEqualTo(1);
    }
}
