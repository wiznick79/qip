package io.github.wiznick79.qip.knowledge.internal.application;

public class DocumentIndexingException extends RuntimeException {
    public DocumentIndexingException(String message) {
        super(message);
    }

    public DocumentIndexingException(String message, Throwable cause) {
        super(message, cause);
    }
}
