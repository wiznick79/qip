package io.github.wiznick79.qip.knowledge.internal.infrastructure.embedding;

import io.github.wiznick79.qip.knowledge.internal.application.Embedding;
import io.github.wiznick79.qip.knowledge.internal.application.EmbeddingGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!spring-ai")
public class DeterministicFakeEmbeddingGenerator implements EmbeddingGenerator {

    private static final String MODEL_ID = "deterministic-hash-v1";

    private final int dimensions;

    public DeterministicFakeEmbeddingGenerator(@Value("${qip.knowledge.fake-embedding-dimensions}") int dimensions) {
        if (dimensions < 8 || dimensions > 2_048) {
            throw new IllegalArgumentException("fake embedding dimensions must be between 8 and 2048");
        }
        this.dimensions = dimensions;
    }

    @Override
    public String modelId() {
        return MODEL_ID;
    }

    @Override
    public List<Embedding> embed(List<String> texts) {
        return texts.stream().map(this::embedOne).toList();
    }

    private Embedding embedOne(String text) {
        float[] values = new float[dimensions];
        String normalized = text.toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split("[^\\p{L}\\p{N}]+", -1);
        int used = 0;
        for (String token : tokens) {
            if (!token.isBlank()) {
                addToken(values, token);
                used++;
            }
        }
        if (used == 0) {
            addToken(values, normalized);
        }
        double norm = 0;
        for (float value : values) {
            norm += value * value;
        }
        if (norm == 0) {
            values[(digest(normalized)[0] & 0xff) % dimensions] = 1.0F;
            norm = 1.0;
        }
        double divisor = Math.sqrt(norm);
        List<Float> result = new ArrayList<>(dimensions);
        for (float value : values) {
            result.add((float) (value / divisor));
        }
        return new Embedding(result);
    }

    private void addToken(float[] values, String token) {
        byte[] hash = digest(token);
        int index = ((hash[0] & 0xff) << 8 | (hash[1] & 0xff)) % dimensions;
        values[index] += (hash[2] & 1) == 0 ? 1.0F : -1.0F;
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
