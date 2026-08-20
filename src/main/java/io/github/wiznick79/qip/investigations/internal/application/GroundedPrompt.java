package io.github.wiznick79.qip.investigations.internal.application;

import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import java.util.List;

public record GroundedPrompt(String version, String text, List<RetrievedPassage> passages) {
    public GroundedPrompt {
        passages = List.copyOf(passages);
    }
}
