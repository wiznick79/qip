package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.api.DocumentSnapshot;

public record UploadDocumentResult(DocumentSnapshot document, boolean created) {}
