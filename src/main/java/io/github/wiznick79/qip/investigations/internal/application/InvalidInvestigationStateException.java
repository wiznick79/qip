package io.github.wiznick79.qip.investigations.internal.application;

public class InvalidInvestigationStateException extends RuntimeException {
    public InvalidInvestigationStateException(String message) {
        super(message);
    }
}
