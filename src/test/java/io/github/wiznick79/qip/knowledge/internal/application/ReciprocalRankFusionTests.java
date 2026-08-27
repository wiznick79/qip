package io.github.wiznick79.qip.knowledge.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReciprocalRankFusionTests {

    private static final UUID DOCUMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");

    @Test
    void promotesEvidenceFoundByBothRankingsAndReturnsNormalizedScores() {
        RetrievedPassage semanticOnly = passage("00000000-0000-0000-0000-000000000001", "semantic");
        RetrievedPassage shared = passage("00000000-0000-0000-0000-000000000002", "shared");
        RetrievedPassage lexicalOnly = passage("00000000-0000-0000-0000-000000000003", "lexical");

        List<RetrievedPassage> result =
                ReciprocalRankFusion.fuse(List.of(semanticOnly, shared), List.of(lexicalOnly, shared), 3);

        assertThat(result)
                .extracting(RetrievedPassage::passageId)
                .containsExactly(shared.passageId(), semanticOnly.passageId(), lexicalOnly.passageId());
        assertThat(result.getFirst().score()).isBetween(0.98, 1.0);
        assertThat(result).allSatisfy(passage -> assertThat(passage.score()).isBetween(0.0, 1.0));
    }

    @Test
    void appliesTheRequestedBoundAndUsesPassageIdForStableTies() {
        RetrievedPassage higherId = passage("00000000-0000-0000-0000-000000000020", "higher");
        RetrievedPassage lowerId = passage("00000000-0000-0000-0000-000000000010", "lower");

        List<RetrievedPassage> result = ReciprocalRankFusion.fuse(List.of(higherId), List.of(lowerId), 1);

        assertThat(result).extracting(RetrievedPassage::passageId).containsExactly(lowerId.passageId());
    }

    private RetrievedPassage passage(String id, String text) {
        return new RetrievedPassage(UUID.fromString(id), DOCUMENT_ID, "Synthetic manual", 1, 0, text, 0.5);
    }
}
