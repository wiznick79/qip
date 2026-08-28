package io.github.wiznick79.qip.investigations.internal.infrastructure;

import io.github.wiznick79.qip.incidents.api.IncidentEvidenceSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentObservationSnapshot;
import io.github.wiznick79.qip.investigations.api.CitationSnapshot;
import io.github.wiznick79.qip.investigations.api.FindingReviewEventSnapshot;
import io.github.wiznick79.qip.investigations.api.FindingSnapshot;
import io.github.wiznick79.qip.investigations.api.QuestionAnswerSnapshot;
import io.github.wiznick79.qip.investigations.internal.application.InvestigationReportData;
import io.github.wiznick79.qip.investigations.internal.application.InvestigationReportRenderer;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

@Component
class PdfBoxInvestigationReportRenderer implements InvestigationReportRenderer {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    @Override
    public byte[] render(InvestigationReportData data) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ReportCanvas canvas = new ReportCanvas(document);
            canvas.title("QIP Investigation Report", "Closed case record");
            canvas.meta("Investigation ID", data.investigation().id().toString());
            canvas.meta("Generated", format(data.generatedAt()));

            var incident = data.incidentCase().incident();
            canvas.section("Case summary");
            canvas.meta("Asset", data.asset().name() + " (" + data.asset().type() + ")");
            canvas.meta("Incident", incident.title());
            canvas.meta("Severity / status", incident.severity() + " / " + incident.status());
            canvas.meta("Occurred", format(incident.occurredAt()));
            canvas.paragraph(
                    incident.description() == null ? "No incident description was recorded." : incident.description());

            canvas.section("Observations");
            if (data.incidentCase().observations().isEmpty()) {
                canvas.muted("No observations were recorded.");
            }
            for (IncidentObservationSnapshot observation : data.incidentCase().observations()) {
                canvas.itemHeading(format(observation.observedAt()) + " - " + observation.authorReference());
                canvas.paragraph(observation.text());
            }

            canvas.section("Evidence");
            if (data.incidentCase().evidence().isEmpty()) {
                canvas.muted("No evidence items were recorded.");
            }
            for (IncidentEvidenceSnapshot evidence : data.incidentCase().evidence()) {
                canvas.itemHeading(evidence.type() + " - " + evidence.provenance());
                canvas.paragraph(evidence.summary());
                canvas.muted("Source: " + evidence.sourceReference() + " | Submitted by: " + evidence.submittedBy()
                        + " | Event: " + format(evidence.eventAt()));
            }

            canvas.section("Grounded questions");
            if (data.investigation().questions().isEmpty()) {
                canvas.muted("No questions were recorded.");
            }
            for (QuestionAnswerSnapshot question : data.investigation().questions()) {
                canvas.itemHeading(question.status() + " - " + question.question());
                canvas.paragraph(question.answer() == null ? question.failureReason() : question.answer());
                for (CitationSnapshot citation : question.citations()) {
                    canvas.citation("Source: " + citation.documentTitle() + ", page " + citation.pageNumber() + " | "
                            + citation.excerpt());
                }
                canvas.muted("Asked: " + format(question.askedAt()) + " | Prompt: " + question.promptVersion()
                        + (question.modelId() == null ? "" : " | Model: " + question.modelId()));
            }

            canvas.keepTogether(150);
            canvas.section("Human-reviewed findings");
            for (FindingSnapshot finding : data.investigation().findings()) {
                canvas.itemHeading(finding.status() + " - " + finding.summary());
                canvas.muted("Proposed by " + finding.proposedBy() + " at " + format(finding.proposedAt()));
                if (finding.reviewedBy() != null) {
                    canvas.paragraph("Review rationale: " + finding.reviewRationale());
                    canvas.muted("Reviewed by " + finding.reviewedBy() + " at " + format(finding.reviewedAt()));
                }
                for (FindingReviewEventSnapshot event : finding.events()) {
                    canvas.audit(event.type() + " - " + event.actorReference() + " - " + format(event.occurredAt())
                            + (event.rationale() == null ? "" : " - " + event.rationale()));
                }
            }

            canvas.section("Closure");
            canvas.paragraph(data.investigation().closureSummary());
            canvas.meta("Closed by", data.investigation().closedBy());
            canvas.meta("Closed at", format(data.investigation().closedAt()));
            canvas.note("This report records source-backed decision support and attributed human review. "
                    + "Generated answers are not autonomous root-cause determinations.");
            canvas.finish(data.generatedAt());
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Investigation report generation failed", exception);
        }
    }

    private static String format(Instant value) {
        return value == null ? "Not recorded" : TIMESTAMP.format(value);
    }

    private static final class ReportCanvas {

        private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private static final float MARGIN = 54;
        private static final float CONTENT_WIDTH = PDRectangle.A4.getWidth() - (2 * MARGIN);
        private static final float BOTTOM = 54;

        private final PDDocument document;
        private PDPage page;
        private float y;

        private ReportCanvas(PDDocument document) throws IOException {
            this.document = document;
            addPage();
        }

        void title(String title, String subtitle) throws IOException {
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.setNonStrokingColor(new Color(13, 54, 45));
                stream.addRect(0, PDRectangle.A4.getHeight() - 128, PDRectangle.A4.getWidth(), 128);
                stream.fill();
                text(stream, BOLD, 24, 255, 255, 255, MARGIN, PDRectangle.A4.getHeight() - 72, title);
                text(stream, REGULAR, 11, 198, 235, 103, MARGIN, PDRectangle.A4.getHeight() - 94, subtitle);
            }
            y = PDRectangle.A4.getHeight() - 158;
        }

        void section(String value) throws IOException {
            ensure(42);
            y -= 12;
            line(value, BOLD, 15, 13, 54, 45);
            y -= 4;
        }

        void keepTogether(float height) throws IOException {
            ensure(height);
        }

        void itemHeading(String value) throws IOException {
            ensure(28);
            for (String line : wrap(value, BOLD, 10, CONTENT_WIDTH)) {
                line(line, BOLD, 10, 28, 44, 38);
            }
            y -= 2;
        }

        void meta(String label, String value) throws IOException {
            ensure(18);
            line(label.toUpperCase() + ": " + value, REGULAR, 9, 83, 99, 92);
        }

        void paragraph(String value) throws IOException {
            for (String line : wrap(value == null ? "Not recorded" : value, REGULAR, 10, CONTENT_WIDTH)) {
                ensure(14);
                line(line, REGULAR, 10, 45, 58, 52);
            }
            y -= 7;
        }

        void muted(String value) throws IOException {
            for (String line : wrap(value, REGULAR, 8, CONTENT_WIDTH)) {
                ensure(11);
                line(line, REGULAR, 8, 105, 117, 111);
            }
            y -= 5;
        }

        void citation(String value) throws IOException {
            for (String line : wrap(value, REGULAR, 8, CONTENT_WIDTH - 16)) {
                ensure(11);
                drawText(line, REGULAR, 8, 48, 91, 67, MARGIN + 12, y);
                y -= 11;
            }
            y -= 4;
        }

        void audit(String value) throws IOException {
            muted("Audit: " + value);
        }

        void note(String value) throws IOException {
            ensure(58);
            y -= 8;
            try (PDPageContentStream stream = append()) {
                stream.setNonStrokingColor(new Color(238, 244, 233));
                stream.addRect(MARGIN, y - 40, CONTENT_WIDTH, 48);
                stream.fill();
            }
            float noteY = y - 8;
            for (String line : wrap(value, REGULAR, 8, CONTENT_WIDTH - 24)) {
                drawText(line, REGULAR, 8, 55, 78, 66, MARGIN + 12, noteY);
                noteY -= 11;
            }
            y -= 48;
        }

        void finish(Instant generatedAt) throws IOException {
            int total = document.getNumberOfPages();
            for (int index = 0; index < total; index++) {
                PDPage footerPage = document.getPage(index);
                try (PDPageContentStream stream = new PDPageContentStream(
                        document, footerPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    text(stream, REGULAR, 8, 112, 124, 118, MARGIN, 28, "QIP | Generated " + format(generatedAt));
                    text(
                            stream,
                            REGULAR,
                            8,
                            112,
                            124,
                            118,
                            PDRectangle.A4.getWidth() - 98,
                            28,
                            "Page " + (index + 1) + " of " + total);
                }
            }
        }

        private void line(String value, PDFont font, float size, int red, int green, int blue) throws IOException {
            ensure(size + 5);
            drawText(value, font, size, red, green, blue, MARGIN, y);
            y -= size + 4;
        }

        private void drawText(
                String value, PDFont font, float size, int red, int green, int blue, float x, float baseline)
                throws IOException {
            try (PDPageContentStream stream = append()) {
                text(stream, font, size, red, green, blue, x, baseline, value);
            }
        }

        private static void text(
                PDPageContentStream stream,
                PDFont font,
                float size,
                int red,
                int green,
                int blue,
                float x,
                float baseline,
                String value)
                throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(new Color(red, green, blue));
            stream.newLineAtOffset(x, baseline);
            stream.showText(safe(value));
            stream.endText();
        }

        private List<String> wrap(String value, PDFont font, float size, float width) throws IOException {
            List<String> lines = new ArrayList<>();
            for (String paragraph : safe(value).split("\\R", -1)) {
                String current = "";
                for (String word : paragraph.split("\\s+")) {
                    String candidate = current.isEmpty() ? word : current + " " + word;
                    if (!current.isEmpty() && font.getStringWidth(candidate) / 1000 * size > width) {
                        lines.add(current);
                        current = word;
                    } else {
                        current = candidate;
                    }
                }
                lines.add(current.isEmpty() ? " " : current);
            }
            return lines;
        }

        private void ensure(float height) throws IOException {
            if (y - height < BOTTOM) {
                addPage();
            }
        }

        private void addPage() throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        private PDPageContentStream append() throws IOException {
            return new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
        }

        private static String safe(String value) {
            String normalized =
                    value.replace('\u2013', '-').replace('\u2014', '-').replace('\u2022', '-');
            StringBuilder result = new StringBuilder(normalized.length());
            normalized
                    .codePoints()
                    .forEach(codePoint -> result.append(
                            codePoint >= 32 && codePoint <= 126
                                    ? (char) codePoint
                                    : codePoint == '\n' || codePoint == '\r' ? (char) codePoint : '?'));
            return result.toString();
        }
    }
}
