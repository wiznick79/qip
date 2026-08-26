package io.github.wiznick79.qip.evaluation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class OllamaModelComparisonFixture {

    private static final String HEADER = "id,document_file,question,expected_status,expected_page,review_criteria";

    static final String VERSION = "v2";
    static final Path DIRECTORY = Path.of("samples", "evaluation", VERSION);
    static final Path CASES = DIRECTORY.resolve("model-comparison-cases.csv");

    private OllamaModelComparisonFixture() {}

    static List<ComparisonCase> readCases() throws Exception {
        List<String> lines = Files.readAllLines(CASES);
        if (lines.isEmpty() || !lines.getFirst().equals(HEADER)) {
            throw new IllegalArgumentException("Unexpected model comparison schema in " + CASES);
        }
        return lines.stream()
                .skip(1)
                .filter(line -> !line.isBlank())
                .map(ComparisonCase::parse)
                .toList();
    }

    record ComparisonCase(
            String id,
            String documentFile,
            String question,
            String expectedStatus,
            int expectedPage,
            List<String> reviewCriteria) {

        static ComparisonCase parse(String line) {
            String[] fields = line.split(",", -1);
            if (fields.length != 6) {
                throw new IllegalArgumentException("Invalid model comparison row: " + line);
            }
            return new ComparisonCase(
                    fields[0],
                    fields[1],
                    fields[2],
                    fields[3],
                    Integer.parseInt(fields[4]),
                    List.of(fields[5].split("\\|")));
        }
    }
}
