package io.github.wiznick79.qip.investigations.internal.infrastructure;

import io.github.wiznick79.qip.investigations.internal.application.AnswerGenerationResult;
import io.github.wiznick79.qip.investigations.internal.application.AnswerGenerator;
import io.github.wiznick79.qip.investigations.internal.application.GroundedPrompt;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!spring-ai")
class DeterministicFakeAnswerGenerator implements AnswerGenerator {

    @Override
    public AnswerGenerationResult generate(GroundedPrompt prompt) {
        if (prompt.passages().isEmpty()) {
            return new AnswerGenerationResult(false, "", List.of(), "deterministic-grounded-v1");
        }
        var primary = prompt.passages().getFirst();
        String answer = "The retrieved source for page %d states: %s"
                .formatted(primary.pageNumber(), truncate(primary.text(), 600));
        return new AnswerGenerationResult(
                true,
                answer,
                prompt.passages().stream()
                        .limit(2)
                        .map(passage -> passage.passageId())
                        .toList(),
                "deterministic-grounded-v1");
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
