package io.github.wiznick79.qip.investigations.api;

import java.util.UUID;

public final class InvestigationNotFoundException extends RuntimeException {
    private final UUID investigationId;

    public InvestigationNotFoundException(UUID investigationId) {
        super("Investigation not found: " + investigationId);
        this.investigationId = investigationId;
    }

    public UUID investigationId() {
        return investigationId;
    }
}
