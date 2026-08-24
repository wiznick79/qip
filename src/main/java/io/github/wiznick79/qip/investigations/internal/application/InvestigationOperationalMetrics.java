package io.github.wiznick79.qip.investigations.internal.application;

import io.github.wiznick79.qip.investigations.api.AnswerStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
class InvestigationOperationalMetrics {

    static final String MODEL = "qip.investigations.model";
    static final String ANSWERS = "qip.investigations.answers";

    private final MeterRegistry registry;

    InvestigationOperationalMetrics(MeterRegistry registry) {
        this.registry = registry;
        for (String outcome : new String[] {"success", "failure"}) {
            modelTimer(outcome);
        }
        for (AnswerStatus status : new AnswerStatus[] {
            AnswerStatus.GROUNDED, AnswerStatus.INSUFFICIENT_EVIDENCE, AnswerStatus.TECHNICAL_FAILURE
        }) {
            answerCounter(status);
        }
    }

    Timer.Sample startModel() {
        return Timer.start(registry);
    }

    void recordModel(Timer.Sample sample, String outcome) {
        sample.stop(modelTimer(outcome));
    }

    void recordAnswer(AnswerStatus status) {
        answerCounter(status).increment();
    }

    private Timer modelTimer(String outcome) {
        return Timer.builder(MODEL)
                .description("Grounded-answer model latency")
                .tag("outcome", outcome)
                .register(registry);
    }

    private io.micrometer.core.instrument.Counter answerCounter(AnswerStatus status) {
        return registry.counter(ANSWERS, "status", status.name());
    }
}
