package io.github.wiznick79.qip.knowledge.internal.application;

import java.util.List;

public record Embedding(List<Float> values) {
    public Embedding {
        values = List.copyOf(values);
        if (values.isEmpty() || values.stream().anyMatch(value -> value == null || !Float.isFinite(value))) {
            throw new IllegalArgumentException("embedding values must be non-empty and finite");
        }
    }
}
