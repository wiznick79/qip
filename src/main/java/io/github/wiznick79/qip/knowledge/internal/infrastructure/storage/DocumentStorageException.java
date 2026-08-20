package io.github.wiznick79.qip.knowledge.internal.infrastructure.storage;

public final class DocumentStorageException extends RuntimeException {
    DocumentStorageException(String message) {
        super(message);
    }

    DocumentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
