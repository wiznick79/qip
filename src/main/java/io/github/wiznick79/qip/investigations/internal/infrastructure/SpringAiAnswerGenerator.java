package io.github.wiznick79.qip.investigations.internal.infrastructure;

import io.github.wiznick79.qip.investigations.internal.application.AnswerGenerationException;
import io.github.wiznick79.qip.investigations.internal.application.AnswerGenerationResult;
import io.github.wiznick79.qip.investigations.internal.application.AnswerGenerator;
import io.github.wiznick79.qip.investigations.internal.application.GroundedPrompt;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ollama")
class SpringAiAnswerGenerator implements AnswerGenerator {

    private static final String UUID_EXPRESSION =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final Pattern PARENTHETICAL_CITATION =
            Pattern.compile("\\s*\\([^\\r\\n)]*" + UUID_EXPRESSION + "[^\\r\\n)]*\\)");
    private static final Pattern BRACKETED_CITATION =
            Pattern.compile("\\s*\\[[^\\r\\n]]*" + UUID_EXPRESSION + "[^\\r\\n]]*]");
    private static final Pattern BARE_UUID = Pattern.compile(UUID_EXPRESSION);
    private static final Pattern EMBEDDED_RESPONSE_FIELD = Pattern.compile("(?m)^\\s*(?:STATUS|CITATIONS|ANSWER):");
    private static final Pattern EMBEDDED_STATUS =
            Pattern.compile("(?m)^\\s*(?:GROUNDED|INSUFFICIENT(?:_|\\\\_)EVIDENCE)\\s*$", Pattern.CASE_INSENSITIVE);

    private final ChatModel model;
    private final String modelId;
    private final int maxAnswerCharacters;

    SpringAiAnswerGenerator(
            ChatModel model,
            @Value("${qip.investigations.spring-ai-model-id}") String modelId,
            @Value("${qip.investigations.max-answer-characters}") int maxAnswerCharacters) {
        this.model = model;
        if (modelId == null || modelId.isBlank() || modelId.length() > 120) {
            throw new IllegalArgumentException("Spring AI chat model ID must contain 1 to 120 characters");
        }
        this.modelId = modelId.trim();
        if (maxAnswerCharacters < 100 || maxAnswerCharacters > 20_000) {
            throw new IllegalArgumentException("Maximum answer characters must be between 100 and 20000");
        }
        this.maxAnswerCharacters = maxAnswerCharacters;
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
        String[] parts = stripOptionalMarkdownFence(response).split("\\R", 3);
        if (parts.length != 3
                || !parts[0].startsWith("STATUS:")
                || !parts[1].startsWith("CITATIONS:")
                || !parts[2].startsWith("ANSWER:")) {
            throw new AnswerGenerationException("Answer provider returned an invalid response format");
        }
        String status = parts[0].substring("STATUS:".length()).trim().toUpperCase(Locale.ROOT);
        String rawAnswer = parts[2].substring("ANSWER:".length()).trim();
        if (EMBEDDED_RESPONSE_FIELD.matcher(rawAnswer).find()
                || EMBEDDED_STATUS.matcher(rawAnswer).find()) {
            throw new AnswerGenerationException("Answer provider returned conflicting response blocks");
        }
        String answer = removeMachineCitationIdentifiers(rawAnswer);
        if (answer.isBlank() || answer.length() > maxAnswerCharacters) {
            throw new AnswerGenerationException("Answer provider returned an invalid answer length");
        }
        if (status.equals("INSUFFICIENT_EVIDENCE")) {
            if (!parts[1].substring("CITATIONS:".length()).trim().equalsIgnoreCase("NONE")) {
                throw new AnswerGenerationException("Insufficient-evidence response contained citations");
            }
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

    private static String stripOptionalMarkdownFence(String response) {
        String stripped = response.strip();
        if (!stripped.startsWith("```") || !stripped.endsWith("```")) {
            return stripped;
        }
        int firstLineEnd = stripped.indexOf('\n');
        if (firstLineEnd < 0) {
            return stripped;
        }
        return stripped.substring(firstLineEnd + 1, stripped.length() - 3).strip();
    }

    private static String removeMachineCitationIdentifiers(String answer) {
        String withoutAnnotations = PARENTHETICAL_CITATION.matcher(answer).replaceAll("");
        withoutAnnotations = BRACKETED_CITATION.matcher(withoutAnnotations).replaceAll("");
        withoutAnnotations = BARE_UUID.matcher(withoutAnnotations).replaceAll("the cited source");
        return withoutAnnotations
                .replaceAll("[\\t ]{2,}", " ")
                .replaceAll(" +([,.;:])", "$1")
                .strip();
    }
}
