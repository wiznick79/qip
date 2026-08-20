package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import io.github.wiznick79.qip.investigations.internal.application.AskQuestionCommand;
import io.github.wiznick79.qip.investigations.internal.application.FindingManagement;
import io.github.wiznick79.qip.investigations.internal.application.InvestigationManagement;
import io.github.wiznick79.qip.investigations.internal.application.ProposeFindingCommand;
import io.github.wiznick79.qip.investigations.internal.application.ReviewFindingCommand;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
class InvestigationController {

    private final InvestigationManagement investigations;
    private final FindingManagement findings;

    InvestigationController(InvestigationManagement investigations, FindingManagement findings) {
        this.investigations = investigations;
        this.findings = findings;
    }

    @PostMapping("/incidents/{incidentId}/investigations")
    InvestigationResponse create(@PathVariable UUID incidentId) {
        return InvestigationResponse.from(investigations.create(incidentId));
    }

    @GetMapping("/investigations/{investigationId}")
    InvestigationResponse get(@PathVariable UUID investigationId) {
        return InvestigationResponse.from(investigations.get(investigationId));
    }

    @PostMapping("/investigations/{investigationId}/questions")
    QuestionAnswerResponse ask(@PathVariable UUID investigationId, @Valid @RequestBody AskQuestionRequest request) {
        return QuestionAnswerResponse.from(
                investigations.ask(investigationId, new AskQuestionCommand(request.question(), request.documentIds())));
    }

    @PostMapping("/investigations/{investigationId}/findings")
    @ResponseStatus(HttpStatus.CREATED)
    FindingResponse proposeFinding(
            @PathVariable UUID investigationId, @Valid @RequestBody ProposeFindingRequest request) {
        return FindingResponse.from(findings.propose(
                investigationId,
                new ProposeFindingCommand(request.sourceQuestionId(), request.summary(), request.proposedBy())));
    }

    @PostMapping("/investigations/{investigationId}/findings/{findingId}/reviews")
    FindingResponse reviewFinding(
            @PathVariable UUID investigationId,
            @PathVariable UUID findingId,
            @Valid @RequestBody ReviewFindingRequest request) {
        return FindingResponse.from(findings.review(
                investigationId,
                findingId,
                new ReviewFindingCommand(request.decision(), request.reviewerReference(), request.rationale())));
    }
}
