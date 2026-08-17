package io.github.wiznick79.qip.incidents.internal.application;

import java.util.UUID;

@FunctionalInterface
public interface ObservationIdGenerator {

    UUID nextId();
}
