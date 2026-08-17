package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import io.github.wiznick79.qip.incidents.internal.application.AppendObservationCommand;
import io.github.wiznick79.qip.incidents.internal.application.ObservationManagement;
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
@RequestMapping("/api/incidents/{incidentId}/observations")
class ObservationController {

    private final ObservationManagement observations;

    ObservationController(ObservationManagement observations) {
        this.observations = observations;
    }

    @PostMapping
    ResponseEntity<ObservationResponse> append(
            @PathVariable UUID incidentId, @Valid @RequestBody AppendObservationRequest request) {
        var observation = observations.append(
                incidentId,
                new AppendObservationCommand(request.text(), request.authorReference(), request.observedAt()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ObservationResponse.from(observation));
    }

    @GetMapping
    ObservationPageResponse list(
            @PathVariable UUID incidentId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = observations.list(incidentId, page, size);
        return new ObservationPageResponse(
                result.items().stream().map(ObservationResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements());
    }
}
