package io.github.wiznick79.qip.assets.internal.domain;

import io.github.wiznick79.qip.assets.api.AssetType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Asset(UUID id, String name, AssetType type, String externalReference, Instant createdAt) {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_EXTERNAL_REFERENCE_LENGTH = 100;

    public Asset {
        Objects.requireNonNull(id, "id must not be null");
        name = requiredText(name, "name", MAX_NAME_LENGTH);
        Objects.requireNonNull(type, "type must not be null");
        externalReference = optionalText(externalReference, "externalReference", MAX_EXTERNAL_REFERENCE_LENGTH);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checkedLength(value.trim(), field, maxLength);
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return checkedLength(value.trim(), field, maxLength);
    }

    private static String checkedLength(String value, String field, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
        }
        return value;
    }
}
