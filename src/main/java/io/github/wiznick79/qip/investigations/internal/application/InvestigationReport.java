package io.github.wiznick79.qip.investigations.internal.application;

import java.util.Objects;

public record InvestigationReport(String filename, byte[] content) {

    public InvestigationReport {
        Objects.requireNonNull(filename, "filename is required");
        content = Objects.requireNonNull(content, "content is required").clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
