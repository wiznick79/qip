package io.github.wiznick79.qip.investigations.internal.infrastructure;

import io.github.wiznick79.qip.investigations.internal.application.FindingIdGenerator;
import io.github.wiznick79.qip.investigations.internal.application.InvestigationIdGenerator;
import io.github.wiznick79.qip.investigations.internal.application.QuestionIdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RandomInvestigationIds implements InvestigationIdGenerator, QuestionIdGenerator, FindingIdGenerator {
    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }

    @Override
    public UUID nextFindingId() {
        return UUID.randomUUID();
    }

    @Override
    public UUID nextEventId() {
        return UUID.randomUUID();
    }
}
