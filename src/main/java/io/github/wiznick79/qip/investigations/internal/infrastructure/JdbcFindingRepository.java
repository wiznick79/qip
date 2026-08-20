package io.github.wiznick79.qip.investigations.internal.infrastructure;

import io.github.wiznick79.qip.investigations.api.FindingEventType;
import io.github.wiznick79.qip.investigations.api.FindingStatus;
import io.github.wiznick79.qip.investigations.internal.application.FindingRepository;
import io.github.wiznick79.qip.investigations.internal.application.InvalidFindingException;
import io.github.wiznick79.qip.investigations.internal.domain.FindingReviewEvent;
import io.github.wiznick79.qip.investigations.internal.domain.Investigation;
import io.github.wiznick79.qip.investigations.internal.domain.InvestigationFinding;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcFindingRepository implements FindingRepository {

    private static final String FINDING_COLUMNS = """
            id, investigation_id, source_question_id, summary, status, proposed_by, proposed_at,
            reviewed_by, review_rationale, reviewed_at
            """;

    private final JdbcClient jdbc;

    JdbcFindingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public InvestigationFinding create(
            InvestigationFinding finding, FindingReviewEvent event, Investigation updatedInvestigation) {
        try {
            jdbc.sql("""
                            INSERT INTO investigation_findings (
                                id, investigation_id, source_question_id, summary, status, proposed_by, proposed_at
                            ) VALUES (
                                :id, :investigationId, :sourceQuestionId, :summary, :status, :proposedBy, :proposedAt
                            )
                            """)
                    .param("id", finding.id())
                    .param("investigationId", finding.investigationId())
                    .param("sourceQuestionId", finding.sourceQuestionId())
                    .param("summary", finding.summary())
                    .param("status", finding.status().name())
                    .param("proposedBy", finding.proposedBy())
                    .param("proposedAt", Timestamp.from(finding.proposedAt()))
                    .update();
        } catch (DuplicateKeyException exception) {
            throw new InvalidFindingException("A finding already exists for this grounded answer");
        }
        insertEvent(event);
        touch(updatedInvestigation);
        return finding;
    }

    @Override
    @Transactional
    public InvestigationFinding review(
            InvestigationFinding finding, FindingReviewEvent event, Investigation updatedInvestigation) {
        int changed = jdbc.sql("""
                        UPDATE investigation_findings SET
                            status = :status,
                            reviewed_by = :reviewedBy,
                            review_rationale = :rationale,
                            reviewed_at = :reviewedAt
                        WHERE id = :id AND investigation_id = :investigationId AND status = 'DRAFT'
                        """)
                .param("status", finding.status().name())
                .param("reviewedBy", finding.reviewedBy())
                .param("rationale", finding.reviewRationale())
                .param("reviewedAt", Timestamp.from(finding.reviewedAt()))
                .param("id", finding.id())
                .param("investigationId", finding.investigationId())
                .update();
        if (changed != 1) {
            throw new InvalidFindingException("Only draft findings can be reviewed");
        }
        insertEvent(event);
        touch(updatedInvestigation);
        return finding;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvestigationFinding> findById(UUID investigationId, UUID findingId) {
        return jdbc.sql("SELECT " + FINDING_COLUMNS
                        + " FROM investigation_findings WHERE investigation_id = :investigationId AND id = :id")
                .param("investigationId", investigationId)
                .param("id", findingId)
                .query(JdbcFindingRepository::mapFinding)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvestigationFinding> findBySourceQuestionId(UUID sourceQuestionId) {
        return jdbc.sql("SELECT " + FINDING_COLUMNS
                        + " FROM investigation_findings WHERE source_question_id = :sourceQuestionId")
                .param("sourceQuestionId", sourceQuestionId)
                .query(JdbcFindingRepository::mapFinding)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestigationFinding> findAll(UUID investigationId) {
        return jdbc.sql("""
                        SELECT %s FROM investigation_findings
                        WHERE investigation_id = :investigationId
                        ORDER BY proposed_at, id
                        LIMIT 100
                        """.formatted(FINDING_COLUMNS))
                .param("investigationId", investigationId)
                .query(JdbcFindingRepository::mapFinding)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FindingReviewEvent> findEvents(UUID findingId) {
        return jdbc.sql("""
                        SELECT id, finding_id, event_type, actor_reference, rationale, occurred_at
                        FROM finding_review_events
                        WHERE finding_id = :findingId
                        ORDER BY occurred_at, id
                        """)
                .param("findingId", findingId)
                .query((result, rowNumber) -> new FindingReviewEvent(
                        result.getObject("id", UUID.class),
                        result.getObject("finding_id", UUID.class),
                        FindingEventType.valueOf(result.getString("event_type")),
                        result.getString("actor_reference"),
                        result.getString("rationale"),
                        result.getTimestamp("occurred_at").toInstant()))
                .list();
    }

    private void insertEvent(FindingReviewEvent event) {
        jdbc.sql("""
                        INSERT INTO finding_review_events (
                            id, finding_id, event_type, actor_reference, rationale, occurred_at
                        ) VALUES (
                            :id, :findingId, :eventType, :actorReference, :rationale, :occurredAt
                        )
                        """)
                .param("id", event.id())
                .param("findingId", event.findingId())
                .param("eventType", event.type().name())
                .param("actorReference", event.actorReference())
                .param("rationale", event.rationale())
                .param("occurredAt", Timestamp.from(event.occurredAt()))
                .update();
    }

    private void touch(Investigation investigation) {
        jdbc.sql("UPDATE investigations SET updated_at = :updatedAt WHERE id = :id")
                .param("updatedAt", Timestamp.from(investigation.updatedAt()))
                .param("id", investigation.id())
                .update();
    }

    private static InvestigationFinding mapFinding(ResultSet result, int rowNumber) throws SQLException {
        Timestamp reviewedAt = result.getTimestamp("reviewed_at");
        return new InvestigationFinding(
                result.getObject("id", UUID.class),
                result.getObject("investigation_id", UUID.class),
                result.getObject("source_question_id", UUID.class),
                result.getString("summary"),
                FindingStatus.valueOf(result.getString("status")),
                result.getString("proposed_by"),
                result.getTimestamp("proposed_at").toInstant(),
                result.getString("reviewed_by"),
                result.getString("review_rationale"),
                reviewedAt == null ? null : reviewedAt.toInstant());
    }
}
