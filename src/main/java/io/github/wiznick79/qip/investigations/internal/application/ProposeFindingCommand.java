package io.github.wiznick79.qip.investigations.internal.application;

import java.util.UUID;

public record ProposeFindingCommand(UUID sourceQuestionId, String summary, String proposedBy) {}
