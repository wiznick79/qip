package io.github.wiznick79.qip.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OllamaModelComparisonFixtureTests {

    @Test
    void providesTwelveUniqueReviewableCasesWithAvailableSyntheticSources() throws Exception {
        var cases = OllamaModelComparisonFixture.readCases();

        assertThat(cases).hasSize(12);
        assertThat(cases)
                .extracting(OllamaModelComparisonFixture.ComparisonCase::id)
                .doesNotHaveDuplicates();
        assertThat(cases).allSatisfy(evaluation -> {
            assertThat(evaluation.question()).isNotBlank();
            assertThat(evaluation.reviewCriteria()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(evaluation.expectedStatus()).isIn("GROUNDED", "INSUFFICIENT_EVIDENCE");
            Path source = evaluation.documentFile().endsWith(".pdf")
                    ? Path.of("output", "pdf", evaluation.documentFile())
                    : OllamaModelComparisonFixture.DIRECTORY.resolve(evaluation.documentFile());
            assertThat(Files.isRegularFile(source)).isTrue();
        });
        assertThat(cases)
                .filteredOn(item -> item.expectedStatus().equals("INSUFFICIENT_EVIDENCE"))
                .hasSizeGreaterThanOrEqualTo(2);
    }
}
