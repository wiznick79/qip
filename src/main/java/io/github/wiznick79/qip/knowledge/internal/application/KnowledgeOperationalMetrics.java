package io.github.wiznick79.qip.knowledge.internal.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
class KnowledgeOperationalMetrics {

    static final String INGESTION = "qip.knowledge.ingestion";
    static final String RETRIEVAL = "qip.knowledge.retrieval";

    private final MeterRegistry registry;

    KnowledgeOperationalMetrics(MeterRegistry registry) {
        this.registry = registry;
        for (String stage : new String[] {"extraction", "indexing"}) {
            for (String outcome : new String[] {"success", "failure"}) {
                ingestionTimer(stage, outcome);
            }
        }
        for (String outcome : new String[] {"success", "failure"}) {
            retrievalTimer(outcome);
        }
    }

    Timer.Sample start() {
        return Timer.start(registry);
    }

    void recordIngestion(Timer.Sample sample, String stage, String outcome) {
        sample.stop(ingestionTimer(stage, outcome));
    }

    void recordRetrieval(Timer.Sample sample, String outcome) {
        sample.stop(retrievalTimer(outcome));
    }

    private Timer ingestionTimer(String stage, String outcome) {
        return Timer.builder(INGESTION)
                .description("Document ingestion stage duration")
                .tag("stage", stage)
                .tag("outcome", outcome)
                .register(registry);
    }

    private Timer retrievalTimer(String outcome) {
        return Timer.builder(RETRIEVAL)
                .description("Knowledge retrieval duration")
                .tag("outcome", outcome)
                .register(registry);
    }
}
