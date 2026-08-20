package io.github.wiznick79.qip.investigations.internal.application;

public class InvalidQuestionException extends RuntimeException {
    public InvalidQuestionException(String message) {
        super(message);
    }
}
