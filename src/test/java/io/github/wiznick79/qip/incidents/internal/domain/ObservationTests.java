package io.github.wiznick79.qip.incidents.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObservationTests {

    private static final UUID OBSERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID INCIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000502");
    private static final Instant RECORDED_AT = Instant.parse("2026-08-17T14:00:00Z");

    @Test
    void normalizesTextAndAuthorReference() {
        Observation observation =
                observation("  Oil visible beneath the pump.  ", "  investigator-17  ", RECORDED_AT.minusSeconds(60));

        assertThat(observation.text()).isEqualTo("Oil visible beneath the pump.");
        assertThat(observation.authorReference()).isEqualTo("investigator-17");
    }

    @Test
    void rejectsFutureObservationTime() {
        Instant futureTime = RECORDED_AT.plusSeconds(1);

        assertThatThrownBy(() -> observation("Oil visible beneath the pump.", "investigator-17", futureTime))
                .isInstanceOf(InvalidObservationTimeException.class)
                .hasMessage("Observation time must not be later than its recording time");
    }

    @Test
    void rejectsBlankText() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> observation("  ", "investigator-17", RECORDED_AT))
                .withMessage("text must not be blank");
    }

    @Test
    void rejectsBlankAuthorReference() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> observation("Oil visible beneath the pump.", "  ", RECORDED_AT))
                .withMessage("authorReference must not be blank");
    }

    private Observation observation(String text, String authorReference, Instant observedAt) {
        return new Observation(OBSERVATION_ID, INCIDENT_ID, text, authorReference, observedAt, RECORDED_AT);
    }
}
