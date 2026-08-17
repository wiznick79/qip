package io.github.wiznick79.qip.incidents.internal.application;

import java.time.Instant;

public record AppendObservationCommand(String text, String authorReference, Instant observedAt) {}
