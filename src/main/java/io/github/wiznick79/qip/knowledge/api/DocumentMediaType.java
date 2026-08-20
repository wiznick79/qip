package io.github.wiznick79.qip.knowledge.api;

public enum DocumentMediaType {
    PDF("application/pdf"),
    PLAIN_TEXT("text/plain");

    private final String value;

    DocumentMediaType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
