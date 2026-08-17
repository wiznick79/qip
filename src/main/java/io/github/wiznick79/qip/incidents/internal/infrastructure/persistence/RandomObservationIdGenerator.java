package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import io.github.wiznick79.qip.incidents.internal.application.ObservationIdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RandomObservationIdGenerator implements ObservationIdGenerator {

    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
