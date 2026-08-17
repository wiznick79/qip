package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

record AppendObservationRequest(
        @NotBlank @Size(max = 4000) String text,
        @NotBlank @Size(max = 120) String authorReference,
        @NotNull Instant observedAt) {}
