package io.github.wiznick79.qip.knowledge.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PassageChunkerTests {

    @Test
    void createsBoundedOverlappingPassagesWithoutCrossingPageBoundaries() {
        var chunker = new PassageChunker(100, 25);
        String pageOne = ("alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu ").repeat(4);

        List<PassageDraft> passages =
                chunker.chunk(List.of(new ExtractedPage(1, pageOne), new ExtractedPage(2, "second   page\ntext")));

        assertThat(passages).hasSizeGreaterThan(2);
        assertThat(passages)
                .allSatisfy(passage -> assertThat(passage.text().length()).isLessThanOrEqualTo(100));
        assertThat(passages)
                .extracting(PassageDraft::sequence)
                .containsExactlyElementsOf(
                        IntStream.range(0, passages.size()).boxed().toList());
        assertThat(passages.getLast().pageNumber()).isEqualTo(2);
        assertThat(passages.getLast().text()).isEqualTo("second page text");
        assertThat(sharedWords(passages.get(0).text(), passages.get(1).text())).isNotEmpty();
    }

    private static List<String> sharedWords(String first, String second) {
        List<String> secondWords = List.of(second.split(" "));
        return List.of(first.split(" ")).stream().filter(secondWords::contains).toList();
    }
}
