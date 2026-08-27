package io.github.wiznick79.qip.knowledge.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentIndexerTests {

    private static final UUID DOCUMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");

    @Test
    void embedsInBatchesAndProducesStablePassageIdentity() {
        var embeddings = new RecordingEmbeddings();
        var repository = new RecordingPassages();
        var indexer = indexer(embeddings, repository);
        var pages = List.of(new ExtractedPage(1, ("synthetic bearing vibration guidance ").repeat(10)));

        indexer.index(DOCUMENT_ID, pages);
        List<UUID> firstIds =
                repository.passages.stream().map(KnowledgePassage::id).toList();
        indexer.index(DOCUMENT_ID, pages);

        assertThat(embeddings.batchSizes).containsExactly(2, 2, 1, 2, 2, 1);
        assertThat(repository.replaceCalls).isEqualTo(2);
        assertThat(repository.passages).extracting(KnowledgePassage::id).containsExactlyElementsOf(firstIds);
        assertThat(repository.passages).allSatisfy(passage -> {
            assertThat(passage.embeddingModel()).isEqualTo("test-model");
            assertThat(passage.textSha256()).matches("[0-9a-f]{64}");
        });
    }

    @Test
    void doesNotReplaceExistingPassagesWhenEmbeddingFails() {
        var embeddings = new RecordingEmbeddings();
        embeddings.returnWrongCount = true;
        var repository = new RecordingPassages();

        assertThatThrownBy(() -> indexer(embeddings, repository)
                        .index(DOCUMENT_ID, List.of(new ExtractedPage(1, "searchable synthetic text"))))
                .isInstanceOf(DocumentIndexingException.class)
                .hasMessage("Embedding model returned an unexpected result count");
        assertThat(repository.replaceCalls).isZero();
    }

    @Test
    void preservesExistingPassagesWhenTheProviderFails() {
        var repository = new RecordingPassages();
        EmbeddingGenerator failing = new EmbeddingGenerator() {
            @Override
            public String modelId() {
                return "test-model";
            }

            @Override
            public List<Embedding> embed(List<String> texts) {
                throw new DocumentIndexingException("Embedding provider failed");
            }
        };

        assertThatThrownBy(() -> indexer(failing, repository)
                        .index(DOCUMENT_ID, List.of(new ExtractedPage(1, "searchable synthetic text"))))
                .isInstanceOf(DocumentIndexingException.class)
                .hasMessage("Embedding provider failed");
        assertThat(repository.replaceCalls).isZero();
    }

    private static KnowledgeDocumentIndexer indexer(EmbeddingGenerator embeddings, PassageRepository repository) {
        return new KnowledgeDocumentIndexer(
                new PassageChunker(100, 20),
                embeddings,
                repository,
                Clock.fixed(Instant.parse("2026-08-20T10:00:00Z"), ZoneOffset.UTC),
                2);
    }

    private static final class RecordingEmbeddings implements EmbeddingGenerator {
        private final List<Integer> batchSizes = new ArrayList<>();
        private boolean returnWrongCount;

        @Override
        public String modelId() {
            return "test-model";
        }

        @Override
        public List<Embedding> embed(List<String> texts) {
            batchSizes.add(texts.size());
            if (returnWrongCount) {
                return List.of();
            }
            return texts.stream()
                    .map(text -> new Embedding(List.of(1.0F, 0.0F)))
                    .toList();
        }
    }

    private static final class RecordingPassages implements PassageRepository {
        private List<KnowledgePassage> passages = List.of();
        private int replaceCalls;

        @Override
        public void replaceAll(UUID documentId, List<KnowledgePassage> passages) {
            replaceCalls++;
            this.passages = List.copyOf(passages);
        }

        @Override
        public List<RetrievedPassage> searchSemantic(
                Embedding query, String embeddingModel, Set<UUID> documentIds, int limit) {
            return List.of();
        }

        @Override
        public List<RetrievedPassage> searchLexical(String query, Set<UUID> documentIds, int limit) {
            return List.of();
        }
    }
}
