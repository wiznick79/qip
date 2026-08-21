package io.github.wiznick79.qip.investigations.internal.domain;

import io.github.wiznick79.qip.investigations.api.FindingEventType;
import java.time.Instant;
import java.util.UUID;

public record FindingReviewEvent(
        UUID id, UUID findingId, FindingEventType type, String actorReference, String rationale, Instant occurredAt) {}
