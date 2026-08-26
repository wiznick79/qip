package io.github.wiznick79.qip.investigations.internal.application;

public class AnswerGenerationException extends RuntimeException {
    private final String attemptedModelId;

    public AnswerGenerationException(String message) {
        this(message, null, null);
    }

    public AnswerGenerationException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public AnswerGenerationException(String message, Throwable cause, String attemptedModelId) {
        super(message, cause);
        this.attemptedModelId = attemptedModelId;
    }

    public String attemptedModelId() {
        return attemptedModelId;
    }
}
