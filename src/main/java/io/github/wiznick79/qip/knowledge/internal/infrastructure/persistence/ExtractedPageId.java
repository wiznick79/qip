package io.github.wiznick79.qip.knowledge.internal.infrastructure.persistence;

import java.io.Serializable;
import java.util.UUID;

record ExtractedPageId(UUID documentId, int pageNumber) implements Serializable {}
