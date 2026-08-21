package io.github.wiznick79.qip.investigations.api;

import java.util.UUID;

public class FindingNotFoundException extends RuntimeException {
    private final UUID findingId;

    public FindingNotFoundException(UUID findingId) {
        super("Finding does not exist: " + findingId);
        this.findingId = findingId;
    }

    public UUID findingId() {
        return findingId;
    }
}
