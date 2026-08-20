package io.github.wiznick79.qip.incidents.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceItemTests {

    private static final UUID EVIDENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");
    private static final UUID INCIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000702");
    private static final Instant RECORDED_AT = Instant.parse("2026-08-17T16:00:00Z");

    @Test
    void normalizesSourceAttributionFields() {
        EvidenceItem evidence = evidence(
                "  Pressure measured at 0 bar.  ", "  sensor:pressure-gauge-07  ", "  investigator-17  ", RECORDED_AT);

        assertThat(evidence.summary()).isEqualTo("Pressure measured at 0 bar.");
        assertThat(evidence.sourceReference()).isEqualTo("sensor:pressure-gauge-07");
        assertThat(evidence.submittedBy()).isEqualTo("investigator-17");
    }

    @Test
    void rejectsFutureEvidenceTime() {
        Instant futureTime = RECORDED_AT.plusSeconds(1);

        assertThatThrownBy(() -> evidence(
                        "Pressure measured at 0 bar.", "sensor:pressure-gauge-07", "investigator-17", futureTime))
                .isInstanceOf(InvalidEvidenceTimeException.class)
                .hasMessage("Evidence event time must not be later than its recording time");
    }

    @Test
    void rejectsBlankSummary() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> evidence("  ", "sensor:pressure-gauge-07", "investigator-17", RECORDED_AT))
                .withMessage("summary must not be blank");
    }

    @Test
    void rejectsBlankSourceReference() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> evidence("Pressure measured at 0 bar.", "  ", "investigator-17", RECORDED_AT))
                .withMessage("sourceReference must not be blank");
    }

    private EvidenceItem evidence(String summary, String sourceReference, String submittedBy, Instant eventAt) {
        return new EvidenceItem(
                EVIDENCE_ID,
                INCIDENT_ID,
                EvidenceType.MEASUREMENT,
                summary,
                sourceReference,
                eventAt,
                EvidenceProvenance.HUMAN_ENTERED,
                submittedBy,
                RECORDED_AT);
    }
}
