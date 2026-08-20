package io.github.wiznick79.qip.knowledge.internal.infrastructure.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.knowledge.api.DocumentMediaType;
import io.github.wiznick79.qip.knowledge.internal.application.DocumentExtractionException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.junit.jupiter.api.Test;

class PdfBoxTextExtractorTests {

    @Test
    void extractsPdfTextWithPageLocators() throws IOException {
        var extractor = new PdfBoxTextExtractor(10, 1_000);

        var pages =
                extractor.extract(pdfWithPages("Bearing temperature", "Inspect lubrication"), DocumentMediaType.PDF);

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).pageNumber()).isEqualTo(1);
        assertThat(pages.get(0).text()).contains("Bearing temperature");
        assertThat(pages.get(1).pageNumber()).isEqualTo(2);
        assertThat(pages.get(1).text()).contains("Inspect lubrication");
    }

    @Test
    void rejectsDocumentsBeyondThePageBound() throws IOException {
        var extractor = new PdfBoxTextExtractor(1, 1_000);

        assertThatThrownBy(() -> extractor.extract(pdfWithPages("One", "Two"), DocumentMediaType.PDF))
                .isInstanceOf(DocumentExtractionException.class)
                .hasMessageContaining("page limit");
    }

    @Test
    void rejectsPlainTextBeyondTheCharacterBound() {
        var extractor = new PdfBoxTextExtractor(10, 4);

        assertThatThrownBy(() -> extractor.extract(
                        "12345".getBytes(java.nio.charset.StandardCharsets.UTF_8), DocumentMediaType.PLAIN_TEXT))
                .isInstanceOf(DocumentExtractionException.class)
                .hasMessageContaining("text limit");
    }

    @Test
    void reportsEncryptedPdfAsAControlledUnsupportedInput() throws IOException {
        var extractor = new PdfBoxTextExtractor(10, 1_000);

        assertThatThrownBy(() -> extractor.extract(encryptedPdf(), DocumentMediaType.PDF))
                .isInstanceOf(DocumentExtractionException.class)
                .hasMessage("Encrypted PDF documents are not supported");
    }

    private static byte[] encryptedPdf() throws IOException {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            var policy = new StandardProtectionPolicy("owner-password", "user-password", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            document.save(output);
            return output.toByteArray();
        }
    }

    private static byte[] pdfWithPages(String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(FontName.HELVETICA), 12);
                    stream.newLineAtOffset(72, 720);
                    stream.showText(pageText);
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
