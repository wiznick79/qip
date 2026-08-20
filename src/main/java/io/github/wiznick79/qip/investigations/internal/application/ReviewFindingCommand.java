package io.github.wiznick79.qip.investigations.internal.application;

import io.github.wiznick79.qip.investigations.api.FindingStatus;

public record ReviewFindingCommand(FindingStatus decision, String reviewerReference, String rationale) {}
