package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import io.github.wiznick79.qip.incidents.internal.application.IncidentIdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RandomIncidentIdGenerator implements IncidentIdGenerator {

    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
