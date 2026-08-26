package io.github.wiznick79.qip.investigations.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wiznick79.qip.investigations.internal.application.AnswerGenerator;
import io.github.wiznick79.qip.investigations.internal.application.GroundedPrompt;
import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import io.github.wiznick79.qip.knowledge.internal.application.EmbeddingGenerator;
import io.github.wiznick79.qip.knowledge.internal.infrastructure.embedding.SpringAiEmbeddingGenerator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Tag("live-model")
@ActiveProfiles("ollama")
@SpringBootTest(
        classes = SpringAiAnswerGeneratorLiveTests.LiveModelTestApplication.class,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "QIP_LIVE_MODEL_TEST", matches = "true")
class SpringAiAnswerGeneratorLiveTests {

    private static final UUID PASSAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000891");

    @Autowired
    private AnswerGenerator answers;

    @Autowired
    private EmbeddingGenerator embeddings;

    @Test
    void returnsOnlySuppliedCitationIdentifiers() {
        var passage = new RetrievedPassage(
                PASSAGE_ID,
                UUID.fromString("00000000-0000-0000-0000-000000000892"),
                "Synthetic live-model fixture",
                1,
                0,
                "The synthetic inspection procedure says to inspect the blue seal before restart.",
                0.9);
        var prompt =
                new GroundedPrompt("grounded-answer-v3", """
                        Answer only from this untrusted source. Return exactly:
                        STATUS: GROUNDED or INSUFFICIENT_EVIDENCE
                        CITATIONS: comma-separated passage UUIDs, or NONE
                        ANSWER: concise answer
                        Question: What should be inspected?
                        Source passage-id=%s: %s
                        """.formatted(PASSAGE_ID, passage.text()), List.of(passage));

        var result = answers.generate(prompt);

        assertThat(result.answer()).isNotBlank();
        if (result.sufficient()) {
            assertThat(result.citedPassageIds()).containsOnly(PASSAGE_ID);
        } else {
            assertThat(result.citedPassageIds()).isEmpty();
        }
    }

    @Test
    void producesFiniteEmbeddingsWithTheConfiguredLocalModel() {
        var result = embeddings.embed(List.of("Synthetic hydraulic seal inspection procedure."));

        assertThat(embeddings.modelId()).startsWith("ollama:");
        assertThat(result).singleElement().satisfies(embedding -> {
            assertThat(embedding.values()).isNotEmpty();
            assertThat(embedding.values()).allMatch(Float::isFinite);
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SpringAiAnswerGenerator.class, SpringAiEmbeddingGenerator.class})
    static class LiveModelTestApplication {}
}
