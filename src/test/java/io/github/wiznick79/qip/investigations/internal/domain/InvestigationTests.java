package io.github.wiznick79.qip.investigations.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.investigations.api.InvestigationStatus;
import io.github.wiznick79.qip.investigations.internal.application.InvalidInvestigationStateException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvestigationTests {

    private static final Instant CREATED_AT = Instant.parse("2026-08-20T10:00:00Z");

    @Test
    void closesOnceWithAnImmutableHumanSummary() {
        Investigation closed = open().close(
                        "The confirmed inspection finding is retained for case closure.",
                        "wiznick79",
                        CREATED_AT.plusSeconds(60));

        assertThat(closed.status()).isEqualTo(InvestigationStatus.CLOSED);
        assertThat(closed.closureSummary()).contains("confirmed inspection finding");
        assertThat(closed.closedBy()).isEqualTo("wiznick79");
        assertThatThrownBy(() -> closed.close("Replacement summary", "other-user", CREATED_AT.plusSeconds(120)))
                .isInstanceOf(InvalidInvestigationStateException.class)
                .hasMessage("The investigation is closed and cannot be changed");
    }

    @Test
    void rejectsMissingClosureProvenance() {
        assertThatThrownBy(() -> open().close(" ", "wiznick79", CREATED_AT.plusSeconds(60)))
                .isInstanceOf(InvalidInvestigationStateException.class)
                .hasMessage("Closure summary must contain 1 to 4000 characters");
        assertThatThrownBy(() -> open().close("Valid summary", " ", CREATED_AT.plusSeconds(60)))
                .isInstanceOf(InvalidInvestigationStateException.class)
                .hasMessage("Closed-by reference must contain 1 to 120 characters");
    }

    private static Investigation open() {
        return new Investigation(
                UUID.fromString("00000000-0000-0000-0000-000000000911"),
                UUID.fromString("00000000-0000-0000-0000-000000000912"),
                InvestigationStatus.OPEN,
                null,
                null,
                null,
                CREATED_AT,
                CREATED_AT);
    }
}
