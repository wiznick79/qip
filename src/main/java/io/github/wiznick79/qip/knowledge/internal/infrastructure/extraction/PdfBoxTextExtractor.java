package io.github.wiznick79.qip.knowledge.internal.infrastructure.extraction;

import io.github.wiznick79.qip.knowledge.api.DocumentMediaType;
import io.github.wiznick79.qip.knowledge.internal.application.DocumentExtractionException;
import io.github.wiznick79.qip.knowledge.internal.application.ExtractedPage;
import io.github.wiznick79.qip.knowledge.internal.application.TextExtractor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class PdfBoxTextExtractor implements TextExtractor {

    private final int maxPages;
    private final int maxCharacters;

    PdfBoxTextExtractor(
            @Value("${qip.documents.max-pages}") int maxPages,
            @Value("${qip.documents.max-extracted-characters}") int maxCharacters) {
        this.maxPages = maxPages;
        this.maxCharacters = maxCharacters;
    }

    @Override
    public List<ExtractedPage> extract(byte[] content, DocumentMediaType mediaType) {
        return switch (mediaType) {
            case PDF -> extractPdf(content);
            case PLAIN_TEXT -> extractPlainText(content);
        };
    }

    private List<ExtractedPage> extractPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw new DocumentExtractionException("Encrypted PDF documents are not supported");
            }
            if (document.getNumberOfPages() > maxPages) {
                throw new DocumentExtractionException("PDF exceeds the configured page limit");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            List<ExtractedPage> pages = new ArrayList<>();
            int characters = 0;
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).strip();
                if (!text.isBlank()) {
                    characters += text.length();
                    enforceCharacterLimit(characters);
                    pages.add(new ExtractedPage(page, text));
                }
            }
            return List.copyOf(pages);
        } catch (InvalidPasswordException exception) {
            throw new DocumentExtractionException("Encrypted PDF documents are not supported", exception);
        } catch (IOException exception) {
            throw new DocumentExtractionException("PDF content could not be parsed", exception);
        }
    }

    private List<ExtractedPage> extractPlainText(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8).strip();
        if (text.isBlank()) {
            return List.of();
        }
        enforceCharacterLimit(text.length());
        return List.of(new ExtractedPage(1, text));
    }

    private void enforceCharacterLimit(int characters) {
        if (characters > maxCharacters) {
            throw new DocumentExtractionException("Document exceeds the configured extracted-text limit");
        }
    }
}
