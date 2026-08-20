package io.github.wiznick79.qip.investigations.internal.application;

public interface AnswerGenerator {
    AnswerGenerationResult generate(GroundedPrompt prompt);
}
