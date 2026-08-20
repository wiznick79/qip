package io.github.wiznick79.qip.investigations.internal.application;

import java.util.Set;
import java.util.UUID;

public record AskQuestionCommand(String question, Set<UUID> documentIds) {
    public AskQuestionCommand {
        if (question == null || question.isBlank() || question.trim().length() > 1_000) {
            throw new InvalidQuestionException("Question must contain 1 to 1000 characters");
        }
        question = question.trim();
        documentIds = documentIds == null ? Set.of() : Set.copyOf(documentIds);
        if (documentIds.size() > 50) {
            throw new InvalidQuestionException("At most 50 documents may be selected");
        }
    }
}
