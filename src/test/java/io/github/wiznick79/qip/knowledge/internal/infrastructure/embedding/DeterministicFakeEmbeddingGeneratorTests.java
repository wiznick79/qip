package io.github.wiznick79.qip.knowledge.internal.infrastructure.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DeterministicFakeEmbeddingGeneratorTests {

    @Test
    void isDeterministicNormalizedAndSensitiveToSharedTerms() {
        var embeddings = new DeterministicFakeEmbeddingGenerator(64);

        var result = embeddings.embed(
                List.of("hydraulic pump oil leak", "hydraulic pump seal leak", "packaging conveyor alignment"));

        assertThat(embeddings.embed(List.of("hydraulic pump oil leak")).getFirst())
                .isEqualTo(result.getFirst());
        assertThat(result).allSatisfy(embedding -> {
            assertThat(embedding.values()).hasSize(64);
            double norm = Math.sqrt(embedding.values().stream()
                    .mapToDouble(value -> value * value)
                    .sum());
            assertThat(norm).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.000_01));
        });
        assertThat(cosine(result.get(0).values(), result.get(1).values()))
                .isGreaterThan(cosine(result.get(0).values(), result.get(2).values()));
    }

    private static double cosine(List<Float> left, List<Float> right) {
        double result = 0;
        for (int index = 0; index < left.size(); index++) {
            result += left.get(index) * right.get(index);
        }
        return result;
    }
}
