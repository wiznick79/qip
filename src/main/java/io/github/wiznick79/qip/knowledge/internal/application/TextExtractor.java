package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.api.DocumentMediaType;
import java.util.List;

public interface TextExtractor {
    List<ExtractedPage> extract(byte[] content, DocumentMediaType mediaType);
}
