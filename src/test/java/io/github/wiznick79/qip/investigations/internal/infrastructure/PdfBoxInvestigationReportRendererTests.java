package io.github.wiznick79.qip.investigations.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wiznick79.qip.assets.api.AssetSnapshot;
import io.github.wiznick79.qip.assets.api.AssetType;
import io.github.wiznick79.qip.incidents.api.IncidentCaseSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentEvidenceSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentObservationSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentSeverity;
import io.github.wiznick79.qip.incidents.api.IncidentSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentStatus;
import io.github.wiznick79.qip.investigations.api.AnswerStatus;
import io.github.wiznick79.qip.investigations.api.CitationSnapshot;
import io.github.wiznick79.qip.investigations.api.FindingEventType;
import io.github.wiznick79.qip.investigations.api.FindingReviewEventSnapshot;
import io.github.wiznick79.qip.investigations.api.FindingSnapshot;
import io.github.wiznick79.qip.investigations.api.FindingStatus;
import io.github.wiznick79.qip.investigations.api.InvestigationSnapshot;
import io.github.wiznick79.qip.investigations.api.InvestigationStatus;
import io.github.wiznick79.qip.investigations.api.QuestionAnswerSnapshot;
import io.github.wiznick79.qip.investigations.internal.application.InvestigationReportData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfBoxInvestigationReportRendererTests {

    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

    @Test
    void rendersAReadablePaginatedClosedCaseReport() throws Exception {
        byte[] report = new PdfBoxInvestigationReportRenderer().render(reportData());

        String preview = System.getProperty("qip.report.preview");
        if (preview != null && !preview.isBlank()) {
            Path output = Path.of(preview);
            Files.createDirectories(output.getParent());
            Files.write(output, report);
        }

        try (var document = Loader.loadPDF(report)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(text)
                    .contains("QIP Investigation Report")
                    .contains("Atlas HP-40 Hydraulic Press")
                    .contains("Hydraulic return pressure exceeded the synthetic threshold")
                    .contains("Atlas HP-40 Synthetic Service Manual, page 2")
                    .contains("CONFIRMED - Inspect and replace the return filter")
                    .contains("The confirmed return-filter finding closes this synthetic case")
                    .contains("Page 1 of");
        }
    }

    private static InvestigationReportData reportData() {
        UUID assetId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID incidentId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID investigationId = UUID.fromString("00000000-0000-0000-0000-000000000301");
        UUID questionId = UUID.fromString("00000000-0000-0000-0000-000000000401");
        List<IncidentObservationSnapshot> observations = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            observations.add(new IncidentObservationSnapshot(
                    UUID.nameUUIDFromBytes(("observation-" + index).getBytes()),
                    "Synthetic observation " + (index + 1)
                            + ": return temperature remained elevated while retract speed decreased.",
                    "synthetic-investigator",
                    NOW.minusSeconds(3600L - index * 60L),
                    NOW));
        }
        var incident = new IncidentSnapshot(
                incidentId,
                assetId,
                "Synthetic HP-40 heat and slow retract",
                "Hydraulic return pressure exceeded the synthetic threshold during a controlled test cycle.",
                IncidentSeverity.HIGH,
                IncidentStatus.RESOLVED,
                NOW.minusSeconds(7200),
                NOW.minusSeconds(7000),
                NOW);
        var evidence = new IncidentEvidenceSnapshot(
                UUID.fromString("00000000-0000-0000-0000-000000000501"),
                "MEASUREMENT",
                "Return-filter differential measured 3.1 bar during the synthetic test.",
                "Synthetic gauge SG-14",
                NOW.minusSeconds(3500),
                "HUMAN_ENTERED",
                "synthetic-investigator",
                NOW);
        var citation = new CitationSnapshot(
                UUID.fromString("00000000-0000-0000-0000-000000000601"),
                UUID.fromString("00000000-0000-0000-0000-000000000602"),
                "Atlas HP-40 Synthetic Service Manual",
                2,
                1,
                "Inspect the return filter when differential pressure exceeds 2.5 bar.",
                0.91);
        var question = new QuestionAnswerSnapshot(
                questionId,
                "What should be inspected first?",
                Set.of(),
                AnswerStatus.GROUNDED,
                "Evidence supports inspecting the return filter first; other contributing factors remain uncertain.",
                List.of(citation),
                "deterministic-grounded-v1",
                "grounded-answer-v3",
                3,
                null,
                NOW.minusSeconds(1800),
                NOW.minusSeconds(1700));
        var proposed = new FindingReviewEventSnapshot(
                UUID.fromString("00000000-0000-0000-0000-000000000701"),
                FindingEventType.PROPOSED,
                "synthetic-investigator",
                null,
                NOW.minusSeconds(1600));
        var confirmed = new FindingReviewEventSnapshot(
                UUID.fromString("00000000-0000-0000-0000-000000000702"),
                FindingEventType.CONFIRMED,
                "synthetic-reviewer",
                "The measurement and cited threshold support this bounded finding.",
                NOW.minusSeconds(1200));
        var finding = new FindingSnapshot(
                UUID.fromString("00000000-0000-0000-0000-000000000703"),
                questionId,
                "Inspect and replace the return filter before restart.",
                FindingStatus.CONFIRMED,
                "synthetic-investigator",
                NOW.minusSeconds(1600),
                "synthetic-reviewer",
                "The measurement and cited threshold support this bounded finding.",
                NOW.minusSeconds(1200),
                List.of(proposed, confirmed));
        var investigation = new InvestigationSnapshot(
                investigationId,
                incidentId,
                InvestigationStatus.CLOSED,
                "The confirmed return-filter finding closes this synthetic case. Other factors remain uncertain.",
                "synthetic-investigator",
                NOW.minusSeconds(600),
                List.of(question),
                List.of(finding),
                NOW.minusSeconds(3600),
                NOW.minusSeconds(600));
        return new InvestigationReportData(
                new AssetSnapshot(assetId, "Atlas HP-40 Hydraulic Press", AssetType.MACHINE, "SYN-HP40", NOW),
                new IncidentCaseSnapshot(incident, observations, List.of(evidence)),
                investigation,
                NOW);
    }
}
