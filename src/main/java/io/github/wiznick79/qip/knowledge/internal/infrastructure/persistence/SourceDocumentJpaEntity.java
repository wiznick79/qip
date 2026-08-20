package io.github.wiznick79.qip.knowledge.internal.infrastructure.persistence;

import io.github.wiznick79.qip.knowledge.api.DocumentMediaType;
import io.github.wiznick79.qip.knowledge.api.DocumentStatus;
import io.github.wiznick79.qip.knowledge.internal.domain.SourceDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_documents")
class SourceDocumentJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 40)
    private DocumentMediaType mediaType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "storage_key", nullable = false, length = 100)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", nullable = false, length = 40)
    private DocumentStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SourceDocumentJpaEntity() {}

    private SourceDocumentJpaEntity(SourceDocument document) {
        id = document.id();
        title = document.title();
        originalFilename = document.originalFilename();
        mediaType = document.mediaType();
        sizeBytes = document.sizeBytes();
        checksumSha256 = document.checksumSha256();
        storageKey = document.storageKey();
        status = document.status();
        failureReason = document.failureReason();
        uploadedAt = document.uploadedAt();
        updatedAt = document.updatedAt();
    }

    static SourceDocumentJpaEntity fromDomain(SourceDocument document) {
        return new SourceDocumentJpaEntity(document);
    }

    SourceDocument toDomain() {
        return new SourceDocument(
                id,
                title,
                originalFilename,
                mediaType,
                sizeBytes,
                checksumSha256,
                storageKey,
                status,
                failureReason,
                uploadedAt,
                updatedAt);
    }
}
