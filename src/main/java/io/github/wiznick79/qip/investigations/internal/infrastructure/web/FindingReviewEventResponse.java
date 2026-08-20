package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import io.github.wiznick79.qip.investigations.api.FindingEventType;
import io.github.wiznick79.qip.investigations.api.FindingReviewEventSnapshot;
import java.time.Instant;
import java.util.UUID;

record FindingReviewEventResponse(
        UUID id, FindingEventType type, String actorReference, String rationale, Instant occurredAt) {
    static FindingReviewEventResponse from(FindingReviewEventSnapshot event) {
        return new FindingReviewEventResponse(
                event.id(), event.type(), event.actorReference(), event.rationale(), event.occurredAt());
    }
}
