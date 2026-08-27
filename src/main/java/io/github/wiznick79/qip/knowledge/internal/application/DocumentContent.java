package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.api.DocumentMediaType;
import java.util.Objects;

public record DocumentContent(String originalFilename, DocumentMediaType mediaType, byte[] bytes) {

    public DocumentContent {
        Objects.requireNonNull(originalFilename, "originalFilename is required");
        Objects.requireNonNull(mediaType, "mediaType is required");
        bytes = Objects.requireNonNull(bytes, "bytes are required").clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
