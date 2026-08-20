package io.github.wiznick79.qip.investigations.internal.application;

import java.util.List;
import java.util.UUID;

public record AnswerGenerationResult(boolean sufficient, String answer, List<UUID> citedPassageIds, String modelId) {
    public AnswerGenerationResult {
        citedPassageIds = List.copyOf(citedPassageIds);
    }
}
