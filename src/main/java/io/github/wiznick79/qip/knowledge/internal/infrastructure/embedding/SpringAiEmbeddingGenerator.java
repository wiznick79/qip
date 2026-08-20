package io.github.wiznick79.qip.knowledge.internal.infrastructure.embedding;

import io.github.wiznick79.qip.knowledge.internal.application.DocumentIndexingException;
import io.github.wiznick79.qip.knowledge.internal.application.Embedding;
import io.github.wiznick79.qip.knowledge.internal.application.EmbeddingGenerator;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("spring-ai")
public class SpringAiEmbeddingGenerator implements EmbeddingGenerator {

    private final EmbeddingModel model;
    private final String modelId;

    public SpringAiEmbeddingGenerator(
            EmbeddingModel model, @Value("${qip.knowledge.spring-ai-model-id}") String modelId) {
        this.model = model;
        if (modelId == null || modelId.isBlank() || modelId.length() > 120) {
            throw new IllegalArgumentException("Spring AI model ID must contain 1 to 120 characters");
        }
        this.modelId = modelId.trim();
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public List<Embedding> embed(List<String> texts) {
        try {
            return model.embed(texts).stream()
                    .map(SpringAiEmbeddingGenerator::toEmbedding)
                    .toList();
        } catch (RuntimeException exception) {
            throw new DocumentIndexingException("Embedding provider failed", exception);
        }
    }

    private static Embedding toEmbedding(float[] values) {
        List<Float> result = new ArrayList<>(values.length);
        for (float value : values) {
            result.add(value);
        }
        return new Embedding(result);
    }
}
