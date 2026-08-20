package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import io.github.wiznick79.qip.incidents.internal.application.AppendEvidenceCommand;
import io.github.wiznick79.qip.incidents.internal.application.EvidenceManagement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/incidents/{incidentId}/evidence")
class EvidenceController {

    private final EvidenceManagement evidence;

    EvidenceController(EvidenceManagement evidence) {
        this.evidence = evidence;
    }

    @PostMapping
    ResponseEntity<EvidenceResponse> append(
            @PathVariable UUID incidentId, @Valid @RequestBody AppendEvidenceRequest request) {
        var item = evidence.append(
                incidentId,
                new AppendEvidenceCommand(
                        request.type(),
                        request.summary(),
                        request.sourceReference(),
                        request.eventAt(),
                        request.submittedBy()));
        return ResponseEntity.status(HttpStatus.CREATED).body(EvidenceResponse.from(item));
    }

    @GetMapping
    EvidencePageResponse list(
            @PathVariable UUID incidentId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = evidence.list(incidentId, page, size);
        return new EvidencePageResponse(
                result.items().stream().map(EvidenceResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements());
    }
}
