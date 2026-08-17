package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import io.github.wiznick79.qip.incidents.api.IncidentStatus;
import jakarta.validation.constraints.NotNull;

record UpdateIncidentStatusRequest(@NotNull IncidentStatus status) {}
