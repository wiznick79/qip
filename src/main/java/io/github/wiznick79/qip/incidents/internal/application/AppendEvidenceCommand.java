package io.github.wiznick79.qip.incidents.internal.application;

import io.github.wiznick79.qip.incidents.internal.domain.EvidenceType;
import java.time.Instant;

public record AppendEvidenceCommand(
        EvidenceType type, String summary, String sourceReference, Instant eventAt, String submittedBy) {}
