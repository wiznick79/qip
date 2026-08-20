package io.github.wiznick79.qip.knowledge.internal.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSourceDocumentRepository extends JpaRepository<SourceDocumentJpaEntity, UUID> {
    Optional<SourceDocumentJpaEntity> findByChecksumSha256(String checksumSha256);
}
