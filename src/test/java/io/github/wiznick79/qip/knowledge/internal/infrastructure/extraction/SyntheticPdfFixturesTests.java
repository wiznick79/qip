package io.github.wiznick79.qip.knowledge.internal.infrastructure.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wiznick79.qip.knowledge.api.DocumentMediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SyntheticPdfFixturesTests {

    private final PdfBoxTextExtractor extractor = new PdfBoxTextExtractor(10, 100_000);

    @ParameterizedTest
    @MethodSource("fixtures")
    void extractsSyntheticManualWithStablePageProvenance(String filename, String assetReference, String scenarioText)
            throws IOException {
        byte[] content = Files.readAllBytes(Path.of("output", "pdf", filename));

        var pages = extractor.extract(content, DocumentMediaType.PDF);

        assertThat(pages).hasSize(3);
        assertThat(pages).extracting(page -> page.pageNumber()).containsExactly(1, 2, 3);
        assertThat(pages.get(0).text()).contains(assetReference, "Entirely fictional");
        assertThat(pages.get(1).text()).contains(scenarioText);
        assertThat(pages.get(2).text()).contains("Is there enough evidence to state a confirmed root cause?");
    }

    private static Stream<Arguments> fixtures() {
        return Stream.of(
                Arguments.of("atlas-hp40-service-manual.pdf", "SYN-HP-040", "After 37 minutes of cycling"),
                Arguments.of("cobalt-cx22-maintenance-guide.pdf", "SYN-CX-022", "Inlet pressure is 0.28 bar"),
                Arguments.of("pioneer-pk7-operations-handbook.pdf", "SYN-PK-007", "take-up mismatch is 6 mm"));
    }
}
