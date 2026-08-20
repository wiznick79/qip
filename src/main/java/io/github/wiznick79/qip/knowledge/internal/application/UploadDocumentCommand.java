package io.github.wiznick79.qip.knowledge.internal.application;

public record UploadDocumentCommand(String title, String originalFilename, String declaredMediaType, byte[] content) {
    public UploadDocumentCommand {
        content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }
}
