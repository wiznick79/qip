package io.github.wiznick79.qip.knowledge.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.knowledge.api.DocumentMediaType;
import io.github.wiznick79.qip.knowledge.api.DocumentStatus;
import io.github.wiznick79.qip.knowledge.internal.domain.SourceDocument;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentManagementTests {

    private static final UUID DOCUMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final Instant NOW = Instant.parse("2026-08-20T10:00:00Z");

    private InMemoryRepository repository;
    private InMemoryStorage storage;
    private ConfigurableExtractor extractor;
    private ConfigurableIndexer indexer;
    private SimpleMeterRegistry meterRegistry;
    private DocumentManagement documents;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository();
        storage = new InMemoryStorage();
        extractor = new ConfigurableExtractor();
        indexer = new ConfigurableIndexer();
        meterRegistry = new SimpleMeterRegistry();
        documents = new DocumentManagement(
                repository,
                storage,
                extractor,
                indexer,
                () -> DOCUMENT_ID,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new KnowledgeOperationalMetrics(meterRegistry),
                1_000);
    }

    @Test
    void storesExtractsAndPersistsPagesWithoutWrappingFileWorkInARepositoryOperation() {
        var result = documents.upload(command("C:\\fakepath\\guide.txt", "Synthetic guidance"));

        assertThat(result.created()).isTrue();
        assertThat(result.document().status()).isEqualTo(DocumentStatus.INDEXED);
        assertThat(result.document().originalFilename()).isEqualTo("guide.txt");
        assertThat(result.document().extractedPageCount()).isEqualTo(1);
        assertThat(storage.keys()).containsExactly(DOCUMENT_ID + ".bin");
        assertThat(meterRegistry
                        .get(KnowledgeOperationalMetrics.INGESTION)
                        .tag("stage", "extraction")
                        .tag("outcome", "success")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry
                        .get(KnowledgeOperationalMetrics.INGESTION)
                        .tag("stage", "indexing")
                        .tag("outcome", "success")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(repository.savedStatuses)
                .containsExactly(
                        DocumentStatus.UPLOADED,
                        DocumentStatus.EXTRACTING,
                        DocumentStatus.EXTRACTED,
                        DocumentStatus.INDEXING,
                        DocumentStatus.INDEXED);
    }

    @Test
    void returnsTheExistingDocumentForDuplicateContent() {
        var first = documents.upload(command("first.txt", "same bytes"));
        var second = documents.upload(command("second.txt", "same bytes"));

        assertThat(second.created()).isFalse();
        assertThat(second.document().id()).isEqualTo(first.document().id());
        assertThat(storage.storeCalls).isEqualTo(1);
        assertThat(extractor.calls).isEqualTo(1);
        assertThat(indexer.calls).isEqualTo(1);
    }

    @Test
    void recordsAControlledFailureAndAllowsRetry() {
        extractor.failure = new DocumentExtractionException("Synthetic parse failure");
        var failed = documents.upload(command("guide.txt", "content"));
        extractor.failure = null;

        var retried = documents.retryExtraction(failed.document().id());

        assertThat(failed.document().status()).isEqualTo(DocumentStatus.EXTRACTION_FAILED);
        assertThat(failed.document().failureReason()).isEqualTo("Synthetic parse failure");
        assertThat(retried.status()).isEqualTo(DocumentStatus.INDEXED);
        assertThat(meterRegistry
                        .get(KnowledgeOperationalMetrics.INGESTION)
                        .tag("stage", "extraction")
                        .tag("outcome", "failure")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void recordsAControlledIndexingFailureAndRetriesWithoutExtractingAgain() {
        indexer.failure = new DocumentIndexingException("Synthetic embedding failure");
        var failed = documents.upload(command("guide.txt", "content"));
        indexer.failure = null;

        var retried = documents.retryIndexing(failed.document().id());

        assertThat(failed.document().status()).isEqualTo(DocumentStatus.INDEXING_FAILED);
        assertThat(failed.document().failureReason()).isEqualTo("Synthetic embedding failure");
        assertThat(retried.status()).isEqualTo(DocumentStatus.INDEXED);
        assertThat(extractor.calls).isEqualTo(1);
        assertThat(indexer.calls).isEqualTo(2);
    }

    @Test
    void reindexesAnIndexedDocumentWithoutExtractingAgain() {
        var uploaded = documents.upload(command("guide.txt", "content"));

        var reindexed = documents.retryIndexing(uploaded.document().id());

        assertThat(reindexed.status()).isEqualTo(DocumentStatus.INDEXED);
        assertThat(extractor.calls).isEqualTo(1);
        assertThat(indexer.calls).isEqualTo(2);
        assertThat(repository.savedStatuses).endsWith(DocumentStatus.INDEXING, DocumentStatus.INDEXED);
    }

    @Test
    void rejectsSpoofedAndOversizedUploadsBeforeStorage() {
        assertThatThrownBy(() -> documents.upload(new UploadDocumentCommand(
                        "Guide", "guide.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidDocumentUploadException.class)
                .hasMessageContaining("does not match");

        assertThatThrownBy(() -> documents.upload(
                        new UploadDocumentCommand("Guide", "guide.txt", "text/plain", new byte[1_001])))
                .isInstanceOf(InvalidDocumentUploadException.class)
                .hasMessageContaining("size limit");
        assertThatThrownBy(() -> documents.upload(new UploadDocumentCommand(
                        "Guide", "guide.txt", "text/plain-malicious", "content".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidDocumentUploadException.class)
                .hasMessageContaining("Only PDF and plain-text");
        assertThat(storage.storeCalls).isZero();
    }

    private static UploadDocumentCommand command(String filename, String content) {
        return new UploadDocumentCommand(
                "Synthetic maintenance guide", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    private static final class InMemoryRepository implements DocumentRepository {
        private final Map<UUID, SourceDocument> documents = new HashMap<>();
        private final Map<UUID, List<ExtractedPage>> pages = new HashMap<>();
        private final List<DocumentStatus> savedStatuses = new ArrayList<>();

        @Override
        public SourceDocument save(SourceDocument document) {
            documents.put(document.id(), document);
            savedStatuses.add(document.status());
            return document;
        }

        @Override
        public SourceDocument saveExtraction(SourceDocument document, List<ExtractedPage> extractedPages) {
            pages.put(document.id(), List.copyOf(extractedPages));
            return save(document);
        }

        @Override
        public Optional<SourceDocument> findById(UUID documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        @Override
        public Optional<SourceDocument> findByChecksum(String checksumSha256) {
            return documents.values().stream()
                    .filter(document -> document.checksumSha256().equals(checksumSha256))
                    .findFirst();
        }

        @Override
        public DocumentPage findAll(int page, int size) {
            var items = documents.values().stream()
                    .map(document -> new io.github.wiznick79.qip.knowledge.api.DocumentSnapshot(
                            document.id(),
                            document.title(),
                            document.originalFilename(),
                            document.mediaType(),
                            document.sizeBytes(),
                            document.checksumSha256(),
                            document.status(),
                            document.failureReason(),
                            extractedPageCount(document.id()),
                            document.uploadedAt(),
                            document.updatedAt()))
                    .toList();
            return new DocumentPage(items, page, size, items.size());
        }

        @Override
        public int extractedPageCount(UUID documentId) {
            return pages.getOrDefault(documentId, List.of()).size();
        }

        @Override
        public List<ExtractedPage> findExtractedPages(UUID documentId) {
            return pages.getOrDefault(documentId, List.of());
        }
    }

    private static final class InMemoryStorage implements DocumentStorage {
        private final Map<String, byte[]> content = new HashMap<>();
        private int storeCalls;

        @Override
        public String store(UUID documentId, byte[] bytes) {
            storeCalls++;
            String key = documentId + ".bin";
            content.put(key, bytes.clone());
            return key;
        }

        @Override
        public byte[] read(String storageKey) {
            return content.get(storageKey).clone();
        }

        @Override
        public void delete(String storageKey) {
            content.remove(storageKey);
        }

        List<String> keys() {
            return List.copyOf(content.keySet());
        }
    }

    private static final class ConfigurableExtractor implements TextExtractor {
        private RuntimeException failure;
        private int calls;

        @Override
        public List<ExtractedPage> extract(byte[] content, DocumentMediaType mediaType) {
            calls++;
            if (failure != null) {
                throw failure;
            }
            return List.of(new ExtractedPage(1, new String(content, StandardCharsets.UTF_8)));
        }
    }

    private static final class ConfigurableIndexer implements DocumentIndexer {
        private RuntimeException failure;
        private int calls;

        @Override
        public void index(UUID documentId, List<ExtractedPage> pages) {
            calls++;
            if (failure != null) {
                throw failure;
            }
        }
    }
}
