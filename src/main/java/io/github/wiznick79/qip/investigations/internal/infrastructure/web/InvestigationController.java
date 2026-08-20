package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import io.github.wiznick79.qip.investigations.internal.application.AskQuestionCommand;
import io.github.wiznick79.qip.investigations.internal.application.InvestigationManagement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
class InvestigationController {

    private final InvestigationManagement investigations;

    InvestigationController(InvestigationManagement investigations) {
        this.investigations = investigations;
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
}
