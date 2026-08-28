package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import io.github.wiznick79.qip.investigations.internal.application.AskQuestionCommand;
import io.github.wiznick79.qip.investigations.internal.application.CloseInvestigationCommand;
import io.github.wiznick79.qip.investigations.internal.application.FindingManagement;
import io.github.wiznick79.qip.investigations.internal.application.InvestigationManagement;
import io.github.wiznick79.qip.investigations.internal.application.InvestigationReportService;
import io.github.wiznick79.qip.investigations.internal.application.ProposeFindingCommand;
import io.github.wiznick79.qip.investigations.internal.application.ReviewFindingCommand;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final InvestigationReportService reports;

    InvestigationController(
            InvestigationManagement investigations, FindingManagement findings, InvestigationReportService reports) {
        this.investigations = investigations;
        this.findings = findings;
        this.reports = reports;
    }

    @PostMapping("/incidents/{incidentId}/investigations")
    InvestigationResponse create(@PathVariable UUID incidentId) {
        return InvestigationResponse.from(investigations.create(incidentId));
    }

    @GetMapping("/investigations/{investigationId}")
    InvestigationResponse get(@PathVariable UUID investigationId) {
        return InvestigationResponse.from(investigations.get(investigationId));
    }

    @GetMapping(value = "/investigations/{investigationId}/report", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> report(@PathVariable UUID investigationId) {
        var report = reports.generate(investigationId);
        byte[] content = report.content();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(content.length)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(report.filename())
                                .build()
                                .toString())
                .body(content);
    }

    @PostMapping("/investigations/{investigationId}/questions")
    QuestionAnswerResponse ask(@PathVariable UUID investigationId, @Valid @RequestBody AskQuestionRequest request) {
        return QuestionAnswerResponse.from(
                investigations.ask(investigationId, new AskQuestionCommand(request.question(), request.documentIds())));
    }

    @PostMapping("/investigations/{investigationId}/closure")
    InvestigationResponse close(
            @PathVariable UUID investigationId,
            @Valid @RequestBody CloseInvestigationRequest request,
            Authentication authentication) {
        return InvestigationResponse.from(investigations.close(
                investigationId, new CloseInvestigationCommand(request.summary(), authentication.getName())));
    }

    @PostMapping("/investigations/{investigationId}/findings")
    @ResponseStatus(HttpStatus.CREATED)
    FindingResponse proposeFinding(
            @PathVariable UUID investigationId,
            @Valid @RequestBody ProposeFindingRequest request,
            Authentication authentication) {
        return FindingResponse.from(findings.propose(
                investigationId,
                new ProposeFindingCommand(request.sourceQuestionId(), request.summary(), authentication.getName())));
    }

    @PostMapping("/investigations/{investigationId}/findings/{findingId}/reviews")
    FindingResponse reviewFinding(
            @PathVariable UUID investigationId,
            @PathVariable UUID findingId,
            @Valid @RequestBody ReviewFindingRequest request,
            Authentication authentication) {
        return FindingResponse.from(findings.review(
                investigationId,
                findingId,
                new ReviewFindingCommand(request.decision(), authentication.getName(), request.rationale())));
    }
}
