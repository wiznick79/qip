package io.github.wiznick79.qip.investigations.internal.application;

import java.util.UUID;

public interface FindingIdGenerator {
    UUID nextFindingId();

    UUID nextEventId();
}
