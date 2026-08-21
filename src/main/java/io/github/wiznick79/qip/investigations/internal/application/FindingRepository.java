package io.github.wiznick79.qip.investigations.internal.application;

import io.github.wiznick79.qip.investigations.internal.domain.FindingReviewEvent;
import io.github.wiznick79.qip.investigations.internal.domain.Investigation;
import io.github.wiznick79.qip.investigations.internal.domain.InvestigationFinding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindingRepository {
    InvestigationFinding create(
            InvestigationFinding finding, FindingReviewEvent event, Investigation updatedInvestigation);

    InvestigationFinding review(
            InvestigationFinding finding, FindingReviewEvent event, Investigation updatedInvestigation);

    Optional<InvestigationFinding> findById(UUID investigationId, UUID findingId);

    Optional<InvestigationFinding> findBySourceQuestionId(UUID sourceQuestionId);

    List<InvestigationFinding> findAll(UUID investigationId);

    FindingReviewReadiness reviewReadiness(UUID investigationId);

    List<FindingReviewEvent> findEvents(UUID findingId);
}
