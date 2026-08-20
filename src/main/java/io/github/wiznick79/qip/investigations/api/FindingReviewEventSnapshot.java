package io.github.wiznick79.qip.investigations.api;

import java.time.Instant;
import java.util.UUID;

public record FindingReviewEventSnapshot(
        UUID id, FindingEventType type, String actorReference, String rationale, Instant occurredAt) {}
