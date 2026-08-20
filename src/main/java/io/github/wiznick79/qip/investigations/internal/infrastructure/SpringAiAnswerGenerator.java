package io.github.wiznick79.qip.investigations.internal.infrastructure;

import io.github.wiznick79.qip.investigations.internal.application.AnswerGenerationException;
import io.github.wiznick79.qip.investigations.internal.application.AnswerGenerationResult;
import io.github.wiznick79.qip.investigations.internal.application.AnswerGenerator;
import io.github.wiznick79.qip.investigations.internal.application.GroundedPrompt;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("spring-ai")
class SpringAiAnswerGenerator implements AnswerGenerator {

    private final ChatModel model;
    private final String modelId;

    SpringAiAnswerGenerator(ChatModel model, @Value("${qip.investigations.spring-ai-model-id}") String modelId) {
        this.model = model;
        if (modelId == null || modelId.isBlank() || modelId.length() > 120) {
            throw new IllegalArgumentException("Spring AI chat model ID must contain 1 to 120 characters");
        }
        this.modelId = modelId.trim();
    }

    @Override
    public AnswerGenerationResult generate(GroundedPrompt prompt) {
        try {
            String response = model.call(prompt.text());
            return parse(response);
        } catch (AnswerGenerationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AnswerGenerationException("Answer provider failed", exception);
        }
    }

    private AnswerGenerationResult parse(String response) {
        if (response == null || response.isBlank()) {
            throw new AnswerGenerationException("Answer provider returned an empty response");
        }
        String[] parts = response.strip().split("\\R", 3);
        if (parts.length != 3
                || !parts[0].startsWith("STATUS:")
                || !parts[1].startsWith("CITATIONS:")
                || !parts[2].startsWith("ANSWER:")) {
            throw new AnswerGenerationException("Answer provider returned an invalid response format");
        }
        String status = parts[0].substring("STATUS:".length()).trim().toUpperCase(Locale.ROOT);
        String answer = parts[2].substring("ANSWER:".length()).trim();
        if (status.equals("INSUFFICIENT_EVIDENCE")) {
            return new AnswerGenerationResult(false, answer, List.of(), modelId);
        }
        if (!status.equals("GROUNDED")) {
            throw new AnswerGenerationException("Answer provider returned an unknown status");
        }
        try {
            List<UUID> citations = Arrays.stream(
                            parts[1].substring("CITATIONS:".length()).split(","))
                    .map(String::trim)
                    .map(UUID::fromString)
                    .toList();
            return new AnswerGenerationResult(true, answer, citations, modelId);
        } catch (IllegalArgumentException exception) {
            throw new AnswerGenerationException("Answer provider returned invalid citation identifiers", exception);
        }
    }
}
