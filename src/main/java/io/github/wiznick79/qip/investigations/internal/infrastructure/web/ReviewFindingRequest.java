package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import io.github.wiznick79.qip.investigations.api.FindingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record ReviewFindingRequest(
        @NotNull FindingStatus decision,
        @NotBlank @Size(max = 120) String reviewerReference,
        @NotBlank @Size(max = 1000) String rationale) {}
