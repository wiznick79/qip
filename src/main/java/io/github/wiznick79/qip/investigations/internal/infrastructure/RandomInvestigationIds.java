package io.github.wiznick79.qip.investigations.internal.infrastructure;

import io.github.wiznick79.qip.investigations.internal.application.InvestigationIdGenerator;
import io.github.wiznick79.qip.investigations.internal.application.QuestionIdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RandomInvestigationIds implements InvestigationIdGenerator, QuestionIdGenerator {
    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
