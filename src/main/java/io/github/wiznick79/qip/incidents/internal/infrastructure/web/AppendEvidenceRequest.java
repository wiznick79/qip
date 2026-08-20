package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import io.github.wiznick79.qip.incidents.internal.domain.EvidenceProvenance;
import io.github.wiznick79.qip.incidents.internal.domain.EvidenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.Instant;

record AppendEvidenceRequest(
        @NotNull EvidenceType type,
        @NotBlank @Size(max = 1000) String summary,
        @NotBlank @Size(max = 500) String sourceReference,
        @NotNull Instant eventAt,
        @NotBlank @Size(max = 120) String submittedBy,

        @Null(message = "must be omitted; provenance is assigned by the server") EvidenceProvenance provenance) {}
