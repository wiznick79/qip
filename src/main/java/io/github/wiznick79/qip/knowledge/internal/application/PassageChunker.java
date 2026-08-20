package io.github.wiznick79.qip.knowledge.internal.application;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class PassageChunker {

    private final int chunkSize;
    private final int overlap;

    PassageChunker(
            @Value("${qip.knowledge.chunk-size}") int chunkSize, @Value("${qip.knowledge.chunk-overlap}") int overlap) {
        if (chunkSize < 100 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("chunk size must be at least 100 and overlap must be smaller");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    List<PassageDraft> chunk(List<ExtractedPage> pages) {
        List<PassageDraft> passages = new ArrayList<>();
        int sequence = 0;
        for (ExtractedPage page : pages) {
            String text = page.text().replaceAll("\\s+", " ").trim();
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + chunkSize, text.length());
                if (end < text.length()) {
                    int boundary = text.lastIndexOf(' ', end);
                    if (boundary >= start + chunkSize / 2) {
                        end = boundary;
                    }
                }
                String passageText = text.substring(start, end).trim();
                if (!passageText.isEmpty()) {
                    passages.add(new PassageDraft(sequence++, page.pageNumber(), passageText));
                }
                if (end == text.length()) {
                    break;
                }
                int next = Math.max(start + 1, end - overlap);
                while (next > start && text.charAt(next - 1) != ' ') {
                    next--;
                }
                while (next < text.length() && text.charAt(next) == ' ') {
                    next++;
                }
                start = next >= end ? Math.max(start + 1, end - overlap) : next;
            }
        }
        return List.copyOf(passages);
    }
}
