package io.github.wiznick79.qip.knowledge.internal.infrastructure.persistence;

import io.github.wiznick79.qip.knowledge.internal.application.ExtractedPage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@IdClass(ExtractedPageId.class)
@Table(name = "extracted_document_pages")
class ExtractedPageJpaEntity {

    @Id
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Id
    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    protected ExtractedPageJpaEntity() {}

    ExtractedPageJpaEntity(UUID documentId, ExtractedPage page) {
        this.documentId = documentId;
        pageNumber = page.pageNumber();
        text = page.text();
    }

    ExtractedPage toApplication() {
        return new ExtractedPage(pageNumber, text);
    }
}
