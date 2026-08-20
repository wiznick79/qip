package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

record AskQuestionRequest(
        @NotBlank @Size(max = 1000) String question,
        @Size(max = 50) Set<UUID> documentIds) {}
