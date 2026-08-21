package io.github.wiznick79.qip.investigations.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.investigations.api.FindingStatus;
import io.github.wiznick79.qip.investigations.internal.application.InvalidFindingException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvestigationFindingTests {

    private static final Instant PROPOSED_AT = Instant.parse("2026-08-20T10:00:00Z");

    @Test
    void confirmsADraftWithExplicitReviewerAndRationale() {
        InvestigationFinding confirmed = draft().review(
                        FindingStatus.CONFIRMED,
                        "wiznick79",
                        "The cited source and recorded observation support the finding.",
                        PROPOSED_AT.plusSeconds(60));

        assertThat(confirmed.status()).isEqualTo(FindingStatus.CONFIRMED);
        assertThat(confirmed.reviewedBy()).isEqualTo("wiznick79");
        assertThat(confirmed.reviewRationale()).contains("cited source");
    }

    @Test
    void rejectsInvalidOrRepeatedReviewTransitions() {
        assertThatThrownBy(() -> draft().review(FindingStatus.DRAFT, "wiznick79", "No decision", PROPOSED_AT))
                .isInstanceOf(InvalidFindingException.class)
                .hasMessage("A review decision must be CONFIRMED or REJECTED");

        InvestigationFinding rejected = draft().review(
                        FindingStatus.REJECTED, "wiznick79", "The observation contradicts the proposal.", PROPOSED_AT);
        assertThatThrownBy(() -> rejected.review(
                        FindingStatus.CONFIRMED, "wiznick79", "Attempted overwrite", PROPOSED_AT.plusSeconds(1)))
                .isInstanceOf(InvalidFindingException.class)
                .hasMessage("Only draft findings can be reviewed");
    }

    private static InvestigationFinding draft() {
        return new InvestigationFinding(
                UUID.fromString("00000000-0000-0000-0000-000000000901"),
                UUID.fromString("00000000-0000-0000-0000-000000000902"),
                UUID.fromString("00000000-0000-0000-0000-000000000903"),
                "Inspect the synthetic hydraulic seal.",
                FindingStatus.DRAFT,
                "wiznick79",
                PROPOSED_AT,
                null,
                null,
                null);
    }
}
