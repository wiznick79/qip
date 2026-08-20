package io.github.wiznick79.qip.knowledge.internal.application;

import java.util.UUID;

public interface DocumentStorage {
    String store(UUID documentId, byte[] content);

    byte[] read(String storageKey);

    void delete(String storageKey);
}
