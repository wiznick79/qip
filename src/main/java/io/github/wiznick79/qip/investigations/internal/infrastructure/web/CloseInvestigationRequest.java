package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CloseInvestigationRequest(
        @NotBlank @Size(max = 4000) String summary,
        @NotBlank @Size(max = 120) String closedBy) {}
