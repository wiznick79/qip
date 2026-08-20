package io.github.wiznick79.qip.knowledge.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wiznick79.qip.knowledge.internal.infrastructure.embedding.DeterministicFakeEmbeddingGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class SyntheticRetrievalEvaluationTests {

    private static final Path EVALUATION_SET = Path.of("samples", "evaluation", "grounded-qa.csv");
    private static final Path DOCUMENTS = Path.of("output", "pdf");

    @Test
    void retrievesTheExpectedEvidenceForEveryVersionedSyntheticCase() throws Exception {
        List<EvaluationCase> cases = readCases();
        assertThat(cases).hasSizeGreaterThanOrEqualTo(3);

        var chunker = new PassageChunker(800, 120);
        var embeddings = new DeterministicFakeEmbeddingGenerator(64);
        for (EvaluationCase evaluation : cases) {
            List<PassageDraft> passages = chunker.chunk(extractPages(DOCUMENTS.resolve(evaluation.documentFile())));
            Embedding query = embeddings.embed(List.of(evaluation.question())).getFirst();
            List<Embedding> passageEmbeddings =
                    embeddings.embed(passages.stream().map(PassageDraft::text).toList());

            RankedPassage best = java.util.stream.IntStream.range(0, passages.size())
                    .mapToObj(index ->
                            new RankedPassage(passages.get(index), cosine(query, passageEmbeddings.get(index))))
                    .max(Comparator.comparingDouble(RankedPassage::score))
                    .orElseThrow();

            assertThat(evaluation.expectedStatus()).as(evaluation.id()).isEqualTo("GROUNDED");
            assertThat(best.passage().pageNumber()).as(evaluation.id()).isEqualTo(evaluation.expectedPage());
            assertThat(best.score()).as(evaluation.id()).isGreaterThanOrEqualTo(evaluation.minimumScore());
            String evidence = best.passage().text().toLowerCase(Locale.ROOT);
            assertThat(evaluation.requiredTerms()).as(evaluation.id()).allMatch(evidence::contains);
        }
    }

    private static List<EvaluationCase> readCases() throws Exception {
        List<String> lines = Files.readAllLines(EVALUATION_SET);
        assertThat(lines).isNotEmpty();
        assertThat(lines.getFirst())
                .isEqualTo("id,document_file,question,expected_status,expected_page,minimum_score,required_terms");
        return lines.stream()
                .skip(1)
                .filter(line -> !line.isBlank())
                .map(EvaluationCase::parse)
                .toList();
    }

    private static List<ExtractedPage> extractPages(Path documentPath) throws Exception {
        List<ExtractedPage> pages = new ArrayList<>();
        try (var document = Loader.loadPDF(Files.readAllBytes(documentPath))) {
            var stripper = new PDFTextStripper();
            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String text = stripper.getText(document).strip();
                if (!text.isBlank()) {
                    pages.add(new ExtractedPage(pageNumber, text));
                }
            }
        }
        return List.copyOf(pages);
    }

    private static double cosine(Embedding left, Embedding right) {
        double score = 0;
        for (int index = 0; index < left.values().size(); index++) {
            score += left.values().get(index) * right.values().get(index);
        }
        return score;
    }

    private record RankedPassage(PassageDraft passage, double score) {}

    private record EvaluationCase(
            String id,
            String documentFile,
            String question,
            String expectedStatus,
            int expectedPage,
            double minimumScore,
            List<String> requiredTerms) {

        static EvaluationCase parse(String line) {
            String[] fields = line.split(",", -1);
            if (fields.length != 7) {
                throw new IllegalArgumentException("Invalid evaluation row: " + line);
            }
            return new EvaluationCase(
                    fields[0],
                    fields[1],
                    fields[2],
                    fields[3],
                    Integer.parseInt(fields[4]),
                    Double.parseDouble(fields[5]),
                    List.of(fields[6].split("\\|")));
        }
    }
}
