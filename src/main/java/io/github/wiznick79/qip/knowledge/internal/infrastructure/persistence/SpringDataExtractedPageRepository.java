package io.github.wiznick79.qip.knowledge.internal.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataExtractedPageRepository extends JpaRepository<ExtractedPageJpaEntity, ExtractedPageId> {
    void deleteByDocumentId(UUID documentId);

    long countByDocumentId(UUID documentId);

    List<ExtractedPageJpaEntity> findByDocumentIdOrderByPageNumberAsc(UUID documentId);
}
