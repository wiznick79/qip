package io.github.wiznick79.qip.knowledge.internal.application;

public record ExtractedPage(int pageNumber, String text) {
    public ExtractedPage {
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        text = text.strip();
    }
}
