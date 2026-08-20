package io.github.wiznick79.qip.investigations.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.wiznick79.qip.investigations.internal.application.AnswerGenerationException;
import io.github.wiznick79.qip.investigations.internal.application.GroundedPrompt;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

class SpringAiAnswerGeneratorTests {

    private static final UUID PASSAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000891");

    @Test
    void parsesGroundedResponseInsideAnOptionalMarkdownFence() {
        ChatModel model = mock(ChatModel.class);
        when(model.call(anyString())).thenReturn("""
                ```text
                STATUS: GROUNDED
                CITATIONS: 00000000-0000-0000-0000-000000000891
                ANSWER: Inspect the synthetic seal.
                ```
                """);
        var generator = generator(model);

        var answer = generator.generate(prompt());

        assertThat(answer.sufficient()).isTrue();
        assertThat(answer.answer()).isEqualTo("Inspect the synthetic seal.");
        assertThat(answer.citedPassageIds()).containsExactly(PASSAGE_ID);
        assertThat(answer.modelId()).isEqualTo("ollama:qwen3-coder:30b");
    }

    @Test
    void parsesInsufficientEvidenceWithoutCitations() {
        ChatModel model = mock(ChatModel.class);
        when(model.call(anyString())).thenReturn("""
                STATUS: INSUFFICIENT_EVIDENCE
                CITATIONS: NONE
                ANSWER: The supplied source does not answer the question.
                """);
        var generator = generator(model);

        var answer = generator.generate(prompt());

        assertThat(answer.sufficient()).isFalse();
        assertThat(answer.citedPassageIds()).isEmpty();
    }

    @Test
    void removesMachineCitationIdentifiersFromTheHumanReadableAnswer() {
        UUID secondPassageId = UUID.fromString("00000000-0000-0000-0000-000000000892");
        ChatModel model = mock(ChatModel.class);
        when(model.call(anyString())).thenReturn("""
                STATUS: GROUNDED
                CITATIONS: 00000000-0000-0000-0000-000000000891, 00000000-0000-0000-0000-000000000892
                ANSWER: Inspect the return filter (Passages %s and %s).
                """.formatted(PASSAGE_ID, secondPassageId));
        var generator = generator(model);

        var answer = generator.generate(prompt());

        assertThat(answer.answer()).isEqualTo("Inspect the return filter.");
        assertThat(answer.answer()).doesNotContain(PASSAGE_ID.toString(), secondPassageId.toString());
        assertThat(answer.citedPassageIds()).containsExactly(PASSAGE_ID, secondPassageId);
    }

    @Test
    void rejectsMalformedProviderOutput() {
        ChatModel model = mock(ChatModel.class);
        when(model.call(anyString())).thenReturn("The answer is probably a seal failure.");
        var generator = generator(model);

        assertThatThrownBy(() -> generator.generate(prompt()))
                .isInstanceOf(AnswerGenerationException.class)
                .hasMessage("Answer provider returned an invalid response format");
    }

    @Test
    void rejectsConflictingResponseBlocksAppendedToAnAnswer() {
        ChatModel model = mock(ChatModel.class);
        when(model.call(anyString())).thenReturn("""
                STATUS: GROUNDED
                CITATIONS: 00000000-0000-0000-0000-000000000891
                ANSWER: Inspect the return filter first.
                INSUFFICIENT\\_EVIDENCE
                CITATIONS: NONE
                ANSWER: The evidence does not confirm a root cause.
                """);
        var generator = generator(model);

        assertThatThrownBy(() -> generator.generate(prompt()))
                .isInstanceOf(AnswerGenerationException.class)
                .hasMessage("Answer provider returned conflicting response blocks");
    }

    @Test
    void wrapsProviderFailuresWithoutExposingTheProviderPayload() {
        ChatModel model = mock(ChatModel.class);
        when(model.call(anyString())).thenThrow(new IllegalStateException("sensitive provider detail"));
        var generator = generator(model);

        assertThatThrownBy(() -> generator.generate(prompt()))
                .isInstanceOf(AnswerGenerationException.class)
                .hasMessage("Answer provider failed");
    }

    @Test
    void rejectsEmptyOversizedAndCitedInsufficientAnswers() {
        ChatModel model = mock(ChatModel.class);
        var generator = new SpringAiAnswerGenerator(model, "ollama:qwen3-coder:30b", 100);

        when(model.call(anyString())).thenReturn("""
                STATUS: GROUNDED
                CITATIONS: 00000000-0000-0000-0000-000000000891
                ANSWER:
                """);
        assertThatThrownBy(() -> generator.generate(prompt()))
                .isInstanceOf(AnswerGenerationException.class)
                .hasMessage("Answer provider returned an invalid answer length");

        when(model.call(anyString())).thenReturn("""
                STATUS: GROUNDED
                CITATIONS: 00000000-0000-0000-0000-000000000891
                ANSWER: %s
                """.formatted("x".repeat(101)));
        assertThatThrownBy(() -> generator.generate(prompt()))
                .isInstanceOf(AnswerGenerationException.class)
                .hasMessage("Answer provider returned an invalid answer length");

        when(model.call(anyString())).thenReturn("""
                STATUS: INSUFFICIENT_EVIDENCE
                CITATIONS: 00000000-0000-0000-0000-000000000891
                ANSWER: The source is insufficient.
                """);
        assertThatThrownBy(() -> generator.generate(prompt()))
                .isInstanceOf(AnswerGenerationException.class)
                .hasMessage("Insufficient-evidence response contained citations");
    }

    private static SpringAiAnswerGenerator generator(ChatModel model) {
        return new SpringAiAnswerGenerator(model, "ollama:qwen3-coder:30b", 4_000);
    }

    private static GroundedPrompt prompt() {
        return new GroundedPrompt("grounded-answer-v2", "Synthetic bounded prompt", List.of());
    }
}
