package io.github.wiznick79.qip.incidents.internal.infrastructure.persistence;

import io.github.wiznick79.qip.incidents.internal.application.EvidenceIdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RandomEvidenceIdGenerator implements EvidenceIdGenerator {

    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
