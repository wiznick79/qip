package io.github.wiznick79.qip.investigations.internal.application;

import io.github.wiznick79.qip.investigations.internal.domain.Investigation;
import io.github.wiznick79.qip.investigations.internal.domain.InvestigationQuestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestigationRepository {
    Investigation createIfAbsent(Investigation investigation);

    Optional<Investigation> findById(UUID investigationId);

    Optional<Investigation> findByIncidentId(UUID incidentId);

    InvestigationQuestion startQuestion(InvestigationQuestion question);

    InvestigationQuestion completeQuestion(InvestigationQuestion question, Investigation investigation);

    Optional<InvestigationQuestion> findQuestion(UUID investigationId, UUID questionId);

    List<InvestigationQuestion> findQuestions(UUID investigationId);
}
