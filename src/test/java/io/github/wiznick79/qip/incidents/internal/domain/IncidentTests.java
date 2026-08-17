package io.github.wiznick79.qip.incidents.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wiznick79.qip.incidents.api.IncidentSeverity;
import io.github.wiznick79.qip.incidents.api.IncidentStatus;
import io.github.wiznick79.qip.incidents.api.InvalidIncidentTransitionException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentTests {

    private static final UUID INCIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID ASSET_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final Instant OCCURRED_AT = Instant.parse("2026-08-16T10:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-08-17T10:00:00Z");

    @Test
    void normalizesHumanEnteredText() {
        Incident incident = incident(IncidentStatus.REPORTED, "  Hydraulic pressure loss  ", "  Gauge fell to zero.  ");

        assertThat(incident.title()).isEqualTo("Hydraulic pressure loss");
        assertThat(incident.description()).isEqualTo("Gauge fell to zero.");
    }

    @Test
    void followsNormalLifecycle() {
        Incident reported = incident(IncidentStatus.REPORTED, "Pressure loss", null);

        Incident investigating = reported.transitionTo(IncidentStatus.INVESTIGATING, CREATED_AT.plusSeconds(1));
        Incident resolved = investigating.transitionTo(IncidentStatus.RESOLVED, CREATED_AT.plusSeconds(2));
        Incident closed = resolved.transitionTo(IncidentStatus.CLOSED, CREATED_AT.plusSeconds(3));

        assertThat(closed.status()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(closed.updatedAt()).isEqualTo(CREATED_AT.plusSeconds(3));
    }

    @Test
    void reopensResolvedIncident() {
        Incident resolved = incident(IncidentStatus.RESOLVED, "Pressure loss", null);

        Incident reopened = resolved.transitionTo(IncidentStatus.INVESTIGATING, CREATED_AT.plusSeconds(1));

        assertThat(reopened.status()).isEqualTo(IncidentStatus.INVESTIGATING);
    }

    @Test
    void treatsRepeatedStatusAsIdempotent() {
        Incident reported = incident(IncidentStatus.REPORTED, "Pressure loss", null);

        assertThat(reported.transitionTo(IncidentStatus.REPORTED, CREATED_AT.plusSeconds(1)))
                .isSameAs(reported);
    }

    @Test
    void rejectsSkippedLifecycleState() {
        Incident reported = incident(IncidentStatus.REPORTED, "Pressure loss", null);

        assertThatThrownBy(() -> reported.transitionTo(IncidentStatus.RESOLVED, CREATED_AT.plusSeconds(1)))
                .isInstanceOf(InvalidIncidentTransitionException.class)
                .hasMessage("Incident cannot transition from REPORTED to RESOLVED");
    }

    @Test
    void keepsClosedIncidentTerminal() {
        Incident closed = incident(IncidentStatus.CLOSED, "Pressure loss", null);

        assertThatThrownBy(() -> closed.transitionTo(IncidentStatus.INVESTIGATING, CREATED_AT.plusSeconds(1)))
                .isInstanceOf(InvalidIncidentTransitionException.class);
    }

    @Test
    void rejectsBlankTitle() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> incident(IncidentStatus.REPORTED, "  ", null))
                .withMessage("title must not be blank");
    }

    private Incident incident(IncidentStatus status, String title, String description) {
        return new Incident(
                INCIDENT_ID,
                ASSET_ID,
                title,
                description,
                IncidentSeverity.HIGH,
                status,
                OCCURRED_AT,
                CREATED_AT,
                CREATED_AT);
    }
}
