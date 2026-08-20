package io.github.wiznick79.qip.investigations.internal.application;

public class AnswerGenerationException extends RuntimeException {
    public AnswerGenerationException(String message) {
        super(message);
    }

    public AnswerGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
