package io.github.wiznick79.qip.knowledge.api;

import java.util.UUID;

public final class DocumentNotFoundException extends RuntimeException {

    private final UUID documentId;

    public DocumentNotFoundException(UUID documentId) {
        super("Document not found: " + documentId);
        this.documentId = documentId;
    }

    public UUID documentId() {
        return documentId;
    }
}
