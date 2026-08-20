package io.github.wiznick79.qip.knowledge.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.knowledge.api.DocumentMediaType;
import io.github.wiznick79.qip.knowledge.api.DocumentStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SourceDocumentTests {

    private static final Instant UPLOADED_AT = Instant.parse("2026-08-20T10:00:00Z");

    @Test
    void followsTheExtractionStateMachine() {
        var uploaded = uploadedDocument();

        var extracting = uploaded.startExtraction(UPLOADED_AT.plusSeconds(1));
        var extracted = extracting.completeExtraction(UPLOADED_AT.plusSeconds(2));

        assertThat(extracting.status()).isEqualTo(DocumentStatus.EXTRACTING);
        assertThat(extracted.status()).isEqualTo(DocumentStatus.EXTRACTED);
        assertThat(extracted.failureReason()).isNull();
    }

    @Test
    void permitsRetryAfterAnExtractionFailure() {
        var failed = uploadedDocument()
                .startExtraction(UPLOADED_AT.plusSeconds(1))
                .failExtraction("Unreadable PDF", UPLOADED_AT.plusSeconds(2));

        var retry = failed.startExtraction(UPLOADED_AT.plusSeconds(3));

        assertThat(retry.status()).isEqualTo(DocumentStatus.EXTRACTING);
        assertThat(retry.failureReason()).isNull();
    }

    @Test
    void rejectsCompletingAQueuedDocument() {
        assertThatThrownBy(() -> uploadedDocument().completeExtraction(UPLOADED_AT.plusSeconds(1)))
                .isInstanceOf(InvalidDocumentStateException.class);
    }

    private static SourceDocument uploadedDocument() {
        return new SourceDocument(
                UUID.fromString("00000000-0000-0000-0000-000000000501"),
                "Synthetic maintenance guide",
                "guide.txt",
                DocumentMediaType.PLAIN_TEXT,
                12,
                "a".repeat(64),
                "00000000-0000-0000-0000-000000000501.bin",
                DocumentStatus.UPLOADED,
                null,
                UPLOADED_AT,
                UPLOADED_AT);
    }
}
