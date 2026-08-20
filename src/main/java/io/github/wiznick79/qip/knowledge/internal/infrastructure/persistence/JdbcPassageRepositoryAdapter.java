package io.github.wiznick79.qip.knowledge.internal.infrastructure.persistence;

import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import io.github.wiznick79.qip.knowledge.internal.application.Embedding;
import io.github.wiznick79.qip.knowledge.internal.application.KnowledgePassage;
import io.github.wiznick79.qip.knowledge.internal.application.PassageRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcPassageRepositoryAdapter implements PassageRepository {

    private static final String INSERT_SQL = """
            INSERT INTO knowledge_passages (
                id, document_id, sequence_number, page_number, text, text_sha256,
                embedding, embedding_model, embedding_dimensions, indexed_at
            ) VALUES (
                :id, :documentId, :sequence, :pageNumber, :text, :textSha256,
                CAST(:embedding AS vector), :embeddingModel, :embeddingDimensions, :indexedAt
            )
            """;

    private final JdbcClient jdbc;

    JdbcPassageRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void replaceAll(UUID documentId, List<KnowledgePassage> passages) {
        jdbc.sql("DELETE FROM knowledge_passages WHERE document_id = :documentId")
                .param("documentId", documentId)
                .update();
        for (KnowledgePassage passage : passages) {
            jdbc.sql(INSERT_SQL)
                    .param("id", passage.id())
                    .param("documentId", passage.documentId())
                    .param("sequence", passage.sequence())
                    .param("pageNumber", passage.pageNumber())
                    .param("text", passage.text())
                    .param("textSha256", passage.textSha256())
                    .param("embedding", vectorLiteral(passage.embedding()))
                    .param("embeddingModel", passage.embeddingModel())
                    .param("embeddingDimensions", passage.embedding().values().size())
                    .param("indexedAt", Timestamp.from(passage.indexedAt()))
                    .update();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetrievedPassage> search(Embedding query, String embeddingModel, Set<UUID> documentIds, int limit) {
        String documentFilter = documentIds.isEmpty() ? "" : "AND p.document_id IN (:documentIds)";
        String sql = """
                SELECT p.id, p.document_id, d.title, p.page_number, p.sequence_number, p.text,
                       1 - (p.embedding <=> CAST(:query AS vector)) AS score
                FROM knowledge_passages p
                JOIN source_documents d ON d.id = p.document_id
                WHERE d.ingestion_status = 'INDEXED'
                  AND p.embedding_model = :embeddingModel
                  AND p.embedding_dimensions = :dimensions
                %s
                ORDER BY p.embedding <=> CAST(:query AS vector), p.id
                LIMIT :limit
                """.formatted(documentFilter);
        JdbcClient.StatementSpec statement = jdbc.sql(sql)
                .param("query", vectorLiteral(query))
                .param("embeddingModel", embeddingModel)
                .param("dimensions", query.values().size())
                .param("limit", limit);
        if (!documentIds.isEmpty()) {
            statement = statement.param("documentIds", documentIds);
        }
        return statement
                .query((result, rowNumber) -> new RetrievedPassage(
                        result.getObject("id", UUID.class),
                        result.getObject("document_id", UUID.class),
                        result.getString("title"),
                        result.getInt("page_number"),
                        result.getInt("sequence_number"),
                        result.getString("text"),
                        result.getDouble("score")))
                .list();
    }

    private static String vectorLiteral(Embedding embedding) {
        return embedding.values().stream()
                .map(value -> String.format(Locale.ROOT, "%.9g", value))
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
