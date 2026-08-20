package io.github.wiznick79.qip.knowledge.internal.application;

import java.util.List;

public interface EmbeddingGenerator {
    String modelId();

    List<Embedding> embed(List<String> texts);
}
