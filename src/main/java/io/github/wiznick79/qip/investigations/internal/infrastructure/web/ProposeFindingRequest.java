package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

record ProposeFindingRequest(
        @NotNull UUID sourceQuestionId,
        @NotBlank @Size(max = 2000) String summary) {}
