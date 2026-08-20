package io.github.wiznick79.qip.knowledge.internal.domain;

import io.github.wiznick79.qip.knowledge.api.DocumentStatus;

public final class InvalidDocumentStateException extends RuntimeException {

    private final DocumentStatus currentStatus;

    public InvalidDocumentStateException(DocumentStatus currentStatus) {
        super("Document cannot be extracted from status " + currentStatus);
        this.currentStatus = currentStatus;
    }

    public DocumentStatus currentStatus() {
        return currentStatus;
    }
}
