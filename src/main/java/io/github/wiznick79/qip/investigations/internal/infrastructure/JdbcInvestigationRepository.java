package io.github.wiznick79.qip.investigations.internal.infrastructure;

import io.github.wiznick79.qip.investigations.api.AnswerStatus;
import io.github.wiznick79.qip.investigations.api.CitationSnapshot;
import io.github.wiznick79.qip.investigations.internal.application.InvestigationRepository;
import io.github.wiznick79.qip.investigations.internal.domain.Investigation;
import io.github.wiznick79.qip.investigations.internal.domain.InvestigationQuestion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcInvestigationRepository implements InvestigationRepository {

    private static final String INVESTIGATION_COLUMNS = "id, incident_id, created_at, updated_at";
    private static final String QUESTION_COLUMNS = """
            id, investigation_id, question_text, selected_document_ids, answer_status, answer_text,
            model_id, prompt_version, retrieved_passage_count, failure_reason, asked_at, completed_at
            """;

    private final JdbcClient jdbc;

    JdbcInvestigationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Investigation createIfAbsent(Investigation investigation) {
        jdbc.sql("""
                        INSERT INTO investigations (id, incident_id, created_at, updated_at)
                        VALUES (:id, :incidentId, :createdAt, :updatedAt)
                        ON CONFLICT (incident_id) DO NOTHING
                        """)
                .param("id", investigation.id())
                .param("incidentId", investigation.incidentId())
                .param("createdAt", Timestamp.from(investigation.createdAt()))
                .param("updatedAt", Timestamp.from(investigation.updatedAt()))
                .update();
        return findByIncidentId(investigation.incidentId()).orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Investigation> findById(UUID investigationId) {
        return jdbc.sql("SELECT " + INVESTIGATION_COLUMNS + " FROM investigations WHERE id = :id")
                .param("id", investigationId)
                .query(JdbcInvestigationRepository::mapInvestigation)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Investigation> findByIncidentId(UUID incidentId) {
        return jdbc.sql("SELECT " + INVESTIGATION_COLUMNS + " FROM investigations WHERE incident_id = :incidentId")
                .param("incidentId", incidentId)
                .query(JdbcInvestigationRepository::mapInvestigation)
                .optional();
    }

    @Override
    @Transactional
    public InvestigationQuestion startQuestion(InvestigationQuestion question) {
        jdbc.sql("""
                        INSERT INTO investigation_questions (
                            id, investigation_id, question_text, selected_document_ids, answer_status,
                            prompt_version, retrieved_passage_count, asked_at
                        ) VALUES (
                            :id, :investigationId, :question, CAST(:documentIds AS uuid[]), :status,
                            :promptVersion, :retrievedCount, :askedAt
                        )
                        """)
                .param("id", question.id())
                .param("investigationId", question.investigationId())
                .param("question", question.question())
                .param("documentIds", uuidArrayLiteral(question.selectedDocumentIds()))
                .param("status", question.status().name())
                .param("promptVersion", question.promptVersion())
                .param("retrievedCount", question.retrievedPassageCount())
                .param("askedAt", Timestamp.from(question.askedAt()))
                .update();
        return question;
    }

    @Override
    @Transactional
    public InvestigationQuestion completeQuestion(InvestigationQuestion question, Investigation investigation) {
        jdbc.sql("""
                        UPDATE investigation_questions SET
                            answer_status = :status,
                            answer_text = :answer,
                            model_id = :modelId,
                            retrieved_passage_count = :retrievedCount,
                            failure_reason = :failureReason,
                            completed_at = :completedAt
                        WHERE id = :id AND answer_status = 'PROCESSING'
                        """)
                .param("status", question.status().name())
                .param("answer", question.answer())
                .param("modelId", question.modelId())
                .param("retrievedCount", question.retrievedPassageCount())
                .param("failureReason", question.failureReason())
                .param("completedAt", Timestamp.from(question.completedAt()))
                .param("id", question.id())
                .update();
        for (int ordinal = 0; ordinal < question.citations().size(); ordinal++) {
            CitationSnapshot citation = question.citations().get(ordinal);
            jdbc.sql("""
                            INSERT INTO answer_citations (
                                question_id, ordinal, passage_id, document_id, document_title,
                                page_number, passage_sequence, excerpt, relevance_score
                            ) VALUES (
                                :questionId, :ordinal, :passageId, :documentId, :documentTitle,
                                :pageNumber, :passageSequence, :excerpt, :relevanceScore
                            )
                            """)
                    .param("questionId", question.id())
                    .param("ordinal", ordinal)
                    .param("passageId", citation.passageId())
                    .param("documentId", citation.documentId())
                    .param("documentTitle", citation.documentTitle())
                    .param("pageNumber", citation.pageNumber())
                    .param("passageSequence", citation.passageSequence())
                    .param("excerpt", citation.excerpt())
                    .param("relevanceScore", citation.relevanceScore())
                    .update();
        }
        jdbc.sql("UPDATE investigations SET updated_at = :updatedAt WHERE id = :id")
                .param("updatedAt", Timestamp.from(investigation.updatedAt()))
                .param("id", investigation.id())
                .update();
        return question;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvestigationQuestion> findQuestion(UUID investigationId, UUID questionId) {
        return jdbc.sql("""
                        SELECT %s FROM investigation_questions
                        WHERE investigation_id = :investigationId AND id = :questionId
                        """.formatted(QUESTION_COLUMNS))
                .param("investigationId", investigationId)
                .param("questionId", questionId)
                .query((result, rowNumber) -> mapQuestion(result, findCitations(questionId)))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestigationQuestion> findQuestions(UUID investigationId) {
        List<InvestigationQuestion> questions = jdbc.sql("""
                        SELECT * FROM (
                            SELECT %s FROM investigation_questions
                            WHERE investigation_id = :investigationId
                            ORDER BY asked_at DESC, id DESC
                            LIMIT 100
                        ) recent
                        ORDER BY asked_at, id
                        """.formatted(QUESTION_COLUMNS))
                .param("investigationId", investigationId)
                .query((result, rowNumber) -> mapQuestion(result, List.of()))
                .list();
        return questions.stream()
                .map(question -> new InvestigationQuestion(
                        question.id(),
                        question.investigationId(),
                        question.question(),
                        question.selectedDocumentIds(),
                        question.status(),
                        question.answer(),
                        findCitations(question.id()),
                        question.modelId(),
                        question.promptVersion(),
                        question.retrievedPassageCount(),
                        question.failureReason(),
                        question.askedAt(),
                        question.completedAt()))
                .toList();
    }

    private List<CitationSnapshot> findCitations(UUID questionId) {
        return jdbc.sql("""
                        SELECT passage_id, document_id, document_title, page_number,
                               passage_sequence, excerpt, relevance_score
                        FROM answer_citations
                        WHERE question_id = :questionId
                        ORDER BY ordinal
                        """)
                .param("questionId", questionId)
                .query((result, rowNumber) -> new CitationSnapshot(
                        result.getObject("passage_id", UUID.class),
                        result.getObject("document_id", UUID.class),
                        result.getString("document_title"),
                        result.getInt("page_number"),
                        result.getInt("passage_sequence"),
                        result.getString("excerpt"),
                        result.getDouble("relevance_score")))
                .list();
    }

    private static Investigation mapInvestigation(ResultSet result, int rowNumber) throws SQLException {
        return new Investigation(
                result.getObject("id", UUID.class),
                result.getObject("incident_id", UUID.class),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant());
    }

    private static InvestigationQuestion mapQuestion(ResultSet result, List<CitationSnapshot> citations)
            throws SQLException {
        Timestamp completedAt = result.getTimestamp("completed_at");
        return new InvestigationQuestion(
                result.getObject("id", UUID.class),
                result.getObject("investigation_id", UUID.class),
                result.getString("question_text"),
                uuidSet(result.getArray("selected_document_ids").getArray()),
                AnswerStatus.valueOf(result.getString("answer_status")),
                result.getString("answer_text"),
                citations,
                result.getString("model_id"),
                result.getString("prompt_version"),
                result.getInt("retrieved_passage_count"),
                result.getString("failure_reason"),
                result.getTimestamp("asked_at").toInstant(),
                completedAt == null ? null : completedAt.toInstant());
    }

    private static Set<UUID> uuidSet(Object array) {
        return Set.copyOf(Arrays.asList((UUID[]) array));
    }

    private static String uuidArrayLiteral(Set<UUID> ids) {
        return ids.stream().map(UUID::toString).sorted().collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }
}
