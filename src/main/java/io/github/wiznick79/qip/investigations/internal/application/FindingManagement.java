package io.github.wiznick79.qip.investigations.internal.application;

import io.github.wiznick79.qip.investigations.api.AnswerStatus;
import io.github.wiznick79.qip.investigations.api.FindingEventType;
import io.github.wiznick79.qip.investigations.api.FindingNotFoundException;
import io.github.wiznick79.qip.investigations.api.FindingReviewEventSnapshot;
import io.github.wiznick79.qip.investigations.api.FindingSnapshot;
import io.github.wiznick79.qip.investigations.api.FindingStatus;
import io.github.wiznick79.qip.investigations.api.InvestigationNotFoundException;
import io.github.wiznick79.qip.investigations.internal.domain.FindingReviewEvent;
import io.github.wiznick79.qip.investigations.internal.domain.Investigation;
import io.github.wiznick79.qip.investigations.internal.domain.InvestigationFinding;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FindingManagement {

    private final InvestigationRepository investigations;
    private final FindingRepository findings;
    private final FindingIdGenerator ids;
    private final Clock clock;

    public FindingManagement(
            InvestigationRepository investigations, FindingRepository findings, FindingIdGenerator ids, Clock clock) {
        this.investigations = investigations;
        this.findings = findings;
        this.ids = ids;
        this.clock = clock;
    }

    public FindingSnapshot propose(UUID investigationId, ProposeFindingCommand command) {
        Investigation investigation = findInvestigation(investigationId);
        var question = investigations
                .findQuestion(investigationId, command.sourceQuestionId())
                .orElseThrow(
                        () -> new InvalidFindingException("The source question does not belong to this investigation"));
        if (question.status() != AnswerStatus.GROUNDED || question.citations().isEmpty()) {
            throw new InvalidFindingException("Only a grounded answer with citations can become a finding");
        }
        if (findings.findBySourceQuestionId(question.id()).isPresent()) {
            throw new InvalidFindingException("A finding already exists for this grounded answer");
        }
        Instant now = Instant.now(clock);
        InvestigationFinding finding = new InvestigationFinding(
                ids.nextFindingId(),
                investigationId,
                question.id(),
                command.summary(),
                FindingStatus.DRAFT,
                command.proposedBy(),
                now,
                null,
                null,
                null);
        FindingReviewEvent event = new FindingReviewEvent(
                ids.nextEventId(), finding.id(), FindingEventType.PROPOSED, finding.proposedBy(), null, now);
        Investigation updated = updated(investigation, now);
        return snapshot(findings.create(finding, event, updated));
    }

    public FindingSnapshot review(UUID investigationId, UUID findingId, ReviewFindingCommand command) {
        Investigation investigation = findInvestigation(investigationId);
        InvestigationFinding current = findings.findById(investigationId, findingId)
                .orElseThrow(() -> new FindingNotFoundException(findingId));
        Instant now = Instant.now(clock);
        InvestigationFinding reviewed =
                current.review(command.decision(), command.reviewerReference(), command.rationale(), now);
        FindingReviewEvent event = new FindingReviewEvent(
                ids.nextEventId(),
                findingId,
                FindingEventType.valueOf(reviewed.status().name()),
                reviewed.reviewedBy(),
                reviewed.reviewRationale(),
                now);
        return snapshot(findings.review(reviewed, event, updated(investigation, now)));
    }

    List<FindingSnapshot> list(UUID investigationId) {
        return findings.findAll(investigationId).stream().map(this::snapshot).toList();
    }

    private Investigation findInvestigation(UUID investigationId) {
        return investigations
                .findById(investigationId)
                .orElseThrow(() -> new InvestigationNotFoundException(investigationId));
    }

    private FindingSnapshot snapshot(InvestigationFinding finding) {
        return new FindingSnapshot(
                finding.id(),
                finding.sourceQuestionId(),
                finding.summary(),
                finding.status(),
                finding.proposedBy(),
                finding.proposedAt(),
                finding.reviewedBy(),
                finding.reviewRationale(),
                finding.reviewedAt(),
                findings.findEvents(finding.id()).stream()
                        .map(event -> new FindingReviewEventSnapshot(
                                event.id(),
                                event.type(),
                                event.actorReference(),
                                event.rationale(),
                                event.occurredAt()))
                        .toList());
    }

    private static Investigation updated(Investigation investigation, Instant updatedAt) {
        return new Investigation(investigation.id(), investigation.incidentId(), investigation.createdAt(), updatedAt);
    }
}
