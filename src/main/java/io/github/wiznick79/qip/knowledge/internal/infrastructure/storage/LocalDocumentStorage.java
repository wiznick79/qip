package io.github.wiznick79.qip.knowledge.internal.infrastructure.storage;

import io.github.wiznick79.qip.knowledge.internal.application.DocumentStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class LocalDocumentStorage implements DocumentStorage {

    private final Path root;

    LocalDocumentStorage(@Value("${qip.documents.storage-directory}") String storageDirectory) {
        root = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Override
    public String store(UUID documentId, byte[] content) {
        String storageKey = documentId + ".bin";
        Path target = resolve(storageKey);
        Path temporary = resolve(documentId + ".uploading");
        try {
            Files.createDirectories(root);
            Files.write(temporary, content);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
            return storageKey;
        } catch (IOException exception) {
            tryDelete(temporary);
            throw new DocumentStorageException("Could not store document", exception);
        }
    }

    @Override
    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException exception) {
            throw new DocumentStorageException("Could not read stored document", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        tryDelete(resolve(storageKey));
    }

    private Path resolve(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.getParent().equals(root)) {
            throw new DocumentStorageException("Invalid storage key");
        }
        return target;
    }

    private static void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort compensation; the generated key is never reused.
        }
    }
}
