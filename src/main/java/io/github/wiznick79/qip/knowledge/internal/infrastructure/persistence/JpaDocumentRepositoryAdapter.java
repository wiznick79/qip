package io.github.wiznick79.qip.knowledge.internal.infrastructure.persistence;

import io.github.wiznick79.qip.knowledge.internal.application.DocumentRepository;
import io.github.wiznick79.qip.knowledge.internal.application.ExtractedPage;
import io.github.wiznick79.qip.knowledge.internal.domain.SourceDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaDocumentRepositoryAdapter implements DocumentRepository {

    private final SpringDataSourceDocumentRepository documents;
    private final SpringDataExtractedPageRepository pages;

    JpaDocumentRepositoryAdapter(
            SpringDataSourceDocumentRepository documents, SpringDataExtractedPageRepository pages) {
        this.documents = documents;
        this.pages = pages;
    }

    @Override
    @Transactional
    public SourceDocument save(SourceDocument document) {
        return documents.save(SourceDocumentJpaEntity.fromDomain(document)).toDomain();
    }

    @Override
    @Transactional
    public SourceDocument saveExtraction(SourceDocument document, List<ExtractedPage> extractedPages) {
        pages.deleteByDocumentId(document.id());
        pages.saveAll(extractedPages.stream()
                .map(page -> new ExtractedPageJpaEntity(document.id(), page))
                .toList());
        return documents.save(SourceDocumentJpaEntity.fromDomain(document)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SourceDocument> findById(UUID documentId) {
        return documents.findById(documentId).map(SourceDocumentJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SourceDocument> findByChecksum(String checksumSha256) {
        return documents.findByChecksumSha256(checksumSha256).map(SourceDocumentJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public int extractedPageCount(UUID documentId) {
        return Math.toIntExact(pages.countByDocumentId(documentId));
    }
}
