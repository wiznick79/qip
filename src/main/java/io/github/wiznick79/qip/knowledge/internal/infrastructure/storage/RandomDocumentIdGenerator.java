package io.github.wiznick79.qip.knowledge.internal.infrastructure.storage;

import io.github.wiznick79.qip.knowledge.internal.application.DocumentIdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RandomDocumentIdGenerator implements DocumentIdGenerator {
    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
