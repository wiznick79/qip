package io.github.wiznick79.qip.investigations.internal.infrastructure;

import io.github.wiznick79.qip.knowledge.internal.infrastructure.embedding.SpringAiEmbeddingGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import({SpringAiAnswerGenerator.class, SpringAiEmbeddingGenerator.class})
public class OllamaEvaluationConfiguration {}
