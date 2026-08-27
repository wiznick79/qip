package io.github.wiznick79.qip;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class RagEvaluationFixture {

    private static final String HEADER =
            "id,kind,document_file,question,expected_status," + "expected_page,minimum_score,required_terms";

    public static final String VERSION = "v3";
    public static final Path DIRECTORY = Path.of("samples", "evaluation", VERSION);
    public static final Path CASES = DIRECTORY.resolve("rag-cases.csv");

    private RagEvaluationFixture() {}

    public static List<EvaluationCase> readCases() throws Exception {
        List<String> lines = Files.readAllLines(CASES);
        if (lines.isEmpty() || !lines.getFirst().equals(HEADER)) {
            throw new IllegalArgumentException("Unexpected RAG evaluation schema in " + CASES);
        }
        return lines.stream()
                .skip(1)
                .filter(line -> !line.isBlank())
                .map(EvaluationCase::parse)
                .toList();
    }

    public enum Kind {
        GROUNDED,
        PROMPT_INJECTION,
        UNSUPPORTED_CLAIM,
        UNKNOWN_CITATION,
        INSUFFICIENT_EVIDENCE
    }

    public record EvaluationCase(
            String id,
            Kind kind,
            String documentFile,
            String question,
            String expectedStatus,
            Integer expectedPage,
            Double minimumScore,
            List<String> requiredTerms) {

        static EvaluationCase parse(String line) {
            String[] fields = line.split(",", -1);
            if (fields.length != 8) {
                throw new IllegalArgumentException("Invalid RAG evaluation row: " + line);
            }
            return new EvaluationCase(
                    fields[0],
                    Kind.valueOf(fields[1]),
                    fields[2],
                    fields[3],
                    fields[4],
                    fields[5].isBlank() ? null : Integer.valueOf(fields[5]),
                    fields[6].isBlank() ? null : Double.valueOf(fields[6]),
                    fields[7].isBlank() ? List.of() : List.of(fields[7].split("\\|")));
        }
    }
}
