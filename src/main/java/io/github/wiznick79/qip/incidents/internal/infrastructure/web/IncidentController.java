package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import io.github.wiznick79.qip.incidents.api.IncidentSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentStatus;
import io.github.wiznick79.qip.incidents.internal.application.CreateIncidentCommand;
import io.github.wiznick79.qip.incidents.internal.application.IncidentManagement;
import io.github.wiznick79.qip.incidents.internal.application.IncidentSearchCriteria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/incidents")
class IncidentController {

    private final IncidentManagement incidents;

    IncidentController(IncidentManagement incidents) {
        this.incidents = incidents;
    }

    @PostMapping
    ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest request) {
        IncidentSnapshot created = incidents.createIncident(new CreateIncidentCommand(
                request.assetId(), request.title(), request.description(), request.severity(), request.occurredAt()));
        return ResponseEntity.created(URI.create("/api/incidents/" + created.id()))
                .body(IncidentResponse.from(created));
    }

    @GetMapping("/{incidentId}")
    IncidentResponse get(@PathVariable UUID incidentId) {
        return IncidentResponse.from(incidents.getIncident(incidentId));
    }

    @PatchMapping("/{incidentId}/status")
    IncidentResponse updateStatus(
            @PathVariable UUID incidentId, @Valid @RequestBody UpdateIncidentStatusRequest request) {
        return IncidentResponse.from(incidents.updateStatus(incidentId, request.status()));
    }

    @GetMapping
    IncidentPageResponse search(
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = incidents.search(new IncidentSearchCriteria(assetId, status, from, to, page, size));
        return new IncidentPageResponse(
                result.items().stream().map(IncidentResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements());
    }
}
