from pathlib import Path

from reportlab import rl_config
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    KeepTogether,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf"
rl_config.invariant = 1

INK = colors.HexColor("#17362D")
MUTED = colors.HexColor("#66756F")
LIME = colors.HexColor("#BDEB5D")
PALE = colors.HexColor("#F2F5EF")
LINE = colors.HexColor("#CBD5CE")
WARNING = colors.HexColor("#F7E8C9")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="DocTitle", parent=styles["Title"], fontName="Helvetica-Bold", fontSize=25, leading=29, textColor=INK, alignment=TA_LEFT, spaceAfter=8))
styles.add(ParagraphStyle(name="Subtitle", parent=styles["Normal"], fontName="Helvetica", fontSize=10, leading=15, textColor=MUTED, spaceAfter=16))
styles.add(ParagraphStyle(name="Section", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=15, leading=19, textColor=INK, spaceBefore=8, spaceAfter=8))
styles.add(ParagraphStyle(name="Subsection", parent=styles["Heading3"], fontName="Helvetica-Bold", fontSize=11, leading=14, textColor=INK, spaceBefore=7, spaceAfter=4))
styles.add(ParagraphStyle(name="BodyTextQip", parent=styles["BodyText"], fontName="Helvetica", fontSize=9.5, leading=14, textColor=colors.HexColor("#283C35"), spaceAfter=7))
styles.add(ParagraphStyle(name="Small", parent=styles["BodyText"], fontName="Helvetica", fontSize=8, leading=11, textColor=MUTED))
styles.add(ParagraphStyle(name="Callout", parent=styles["BodyText"], fontName="Helvetica-Bold", fontSize=9, leading=13, textColor=INK, leftIndent=8, rightIndent=8, spaceBefore=6, spaceAfter=6))


MACHINES = [
    {
        "filename": "atlas-hp40-service-manual.pdf",
        "code": "SYN-HP-040",
        "title": "Atlas HP-40",
        "kind": "Hydraulic Press - Synthetic Service Manual",
        "revision": "Revision 1.2 / Synthetic edition",
        "specs": [
            ("Rated forming force", "400 kN"),
            ("Normal hydraulic pressure", "155-165 bar"),
            ("Reservoir volume", "120 L"),
            ("Recommended fluid", "Synthetic ISO VG 46 test fluid"),
            ("Normal oil temperature", "35-58 C"),
            ("Return-filter alarm", "2.5 bar differential"),
        ],
        "overview": "The fictional Atlas HP-40 is a single-station hydraulic forming press used in synthetic acceptance scenarios. Its pressure circuit includes a variable-displacement pump, proportional relief valve, return filter, oil cooler, and guarded ram area.",
        "symptoms": [
            ("H17", "Oil temperature above 63 C for 90 seconds", "Check cooler airflow. If return-filter differential exceeds 2.5 bar, isolate the press and replace the filter element."),
            ("P08", "Commanded pressure not reached", "Inspect for external leakage, confirm reservoir level, then verify relief-valve setting. Do not increase the setting above 165 bar."),
            ("R12", "Ram retract time above 4.8 seconds", "Check return restriction and directional-valve response. A clogged return filter is the preferred first inspection."),
        ],
        "checks": [
            "At shift start: inspect guards, hoses, oil level, and emergency-stop function.",
            "Every 250 synthetic hours: record return-filter differential at 150 bar and clean the cooler intake screen.",
            "Every 1,000 synthetic hours: sample fluid, inspect hose date labels, and verify relief pressure with a calibrated gauge.",
            "After any H17 event: do not reset more than once without identifying the heat source.",
        ],
        "scenario": "Scenario HP-A: After 37 minutes of cycling, oil temperature reaches 66 C and ram retract time rises to 5.4 seconds. The return-filter indicator reads 3.1 bar. Evidence supports inspecting and replacing the return filter before changing the relief-valve setting.",
    },
    {
        "filename": "cobalt-cx22-maintenance-guide.pdf",
        "code": "SYN-CX-022",
        "title": "Cobalt CX-22",
        "kind": "Centrifugal Pump - Synthetic Maintenance Guide",
        "revision": "Revision 2.0 / Synthetic edition",
        "specs": [
            ("Nominal flow", "22 m3/h"),
            ("Rated speed", "2,900 rpm"),
            ("Minimum inlet pressure", "0.40 bar gauge"),
            ("Seal-flush target", "1.6-2.0 L/min"),
            ("Vibration alert", "4.5 mm/s RMS"),
            ("Immediate shutdown", "7.1 mm/s RMS"),
        ],
        "overview": "The fictional Cobalt CX-22 transfers clean synthetic process water. The test assembly includes an inlet strainer, mechanical seal flush, flexible coupling, and two radial vibration measurement points.",
        "symptoms": [
            ("V21", "Broadband vibration above 4.5 mm/s RMS", "Check inlet pressure and strainer condition before alignment. Cavitation is likely when inlet pressure is below 0.40 bar and noise resembles gravel."),
            ("S04", "Seal-flush flow below 1.6 L/min", "Inspect the flush orifice and isolation valve. Restore 1.6-2.0 L/min before prolonged operation."),
            ("B09", "Drive-end bearing temperature above 82 C", "Stop at 90 C. Inspect lubrication quantity and coupling alignment after lockout."),
        ],
        "checks": [
            "Daily: record inlet pressure, discharge pressure, seal-flush flow, and drive-end vibration.",
            "Every 500 synthetic hours: inspect coupling insert and clean the inlet strainer when pressure loss exceeds 0.18 bar.",
            "Every 2,000 synthetic hours: inspect bearing lubricant and verify sensor readings against a portable reference.",
            "After a 7.1 mm/s shutdown: preserve readings and inspect for cavitation damage before restart.",
        ],
        "scenario": "Scenario CX-B: Operators report gravel-like noise and 5.2 mm/s vibration. Inlet pressure is 0.28 bar, while seal-flush flow remains 1.8 L/min. Evidence supports inlet restriction and cavitation as the leading hypothesis; alignment should not be adjusted first.",
    },
    {
        "filename": "pioneer-pk7-operations-handbook.pdf",
        "code": "SYN-PK-007",
        "title": "Pioneer PK-7",
        "kind": "Packaging Conveyor - Synthetic Operations Handbook",
        "revision": "Revision 1.5 / Synthetic edition",
        "specs": [
            ("Belt width", "600 mm"),
            ("Nominal speed", "0.65 m/s"),
            ("Tracking tolerance", "+/- 3 mm"),
            ("Tension-side mismatch limit", "4 mm"),
            ("Photoeye cleaning interval", "Weekly"),
            ("Drive current alert", "6.8 A for 10 seconds"),
        ],
        "overview": "The fictional Pioneer PK-7 moves sealed cartons through a synthetic inspection cell. It uses an encoder, entry photoeye, belt-drift switches, and independently adjustable take-up screws.",
        "symptoms": [
            ("E17", "Right belt-drift switch active", "Stop the conveyor. Remove debris, measure both take-up positions, and correct mismatch only when it exceeds 4 mm. Adjust in quarter-turn increments."),
            ("E06", "Entry photoeye blocked for 12 seconds", "Remove the carton under safe access, clean the lens, and verify bracket alignment. Do not bypass the sensor."),
            ("D31", "Drive current above 6.8 A", "Inspect belt obstruction and roller freedom before changing motor parameters."),
        ],
        "checks": [
            "At shift start: test emergency stops, guards, drift switches, and photoeye response.",
            "Weekly: clean the photoeye lens and measure left/right take-up positions from the fixed datum.",
            "Every 750 synthetic hours: inspect belt splice, roller bearings, and gearbox leakage.",
            "After E17: record the measured tracking offset and take-up mismatch before making an adjustment.",
        ],
        "scenario": "Scenario PK-C: E17 occurs twice after carton debris is removed. Belt offset is 8 mm right and take-up mismatch is 6 mm. Evidence supports correcting take-up asymmetry in quarter-turn increments, then observing five unloaded revolutions before production restart.",
    },
]


def page_header_footer(canvas, document):
    canvas.saveState()
    width, height = A4
    canvas.setFillColor(INK)
    canvas.rect(0, height - 18 * mm, width, 18 * mm, fill=1, stroke=0)
    canvas.setFillColor(LIME)
    canvas.rect(16 * mm, height - 12 * mm, 4 * mm, 4 * mm, fill=1, stroke=0)
    canvas.setFillColor(colors.white)
    canvas.setFont("Helvetica-Bold", 9)
    canvas.drawString(23 * mm, height - 10.8 * mm, "QIP SYNTHETIC EQUIPMENT LIBRARY")
    canvas.setFillColor(MUTED)
    canvas.setFont("Helvetica", 7.5)
    canvas.drawString(16 * mm, 11 * mm, "Fictional equipment and values - safe for demonstrations and automated tests")
    canvas.drawRightString(width - 16 * mm, 11 * mm, f"Page {document.page}")
    canvas.restoreState()


def bullet(text):
    return Paragraph(f"<bullet>&bull;</bullet>{text}", styles["BodyTextQip"])


def make_pdf(machine):
    output_path = OUTPUT / machine["filename"]
    document = SimpleDocTemplate(
        str(output_path),
        pagesize=A4,
        rightMargin=16 * mm,
        leftMargin=16 * mm,
        topMargin=27 * mm,
        bottomMargin=19 * mm,
        title=f"{machine['title']} {machine['kind']}",
        author="Quality Investigation Platform",
        subject="Synthetic technical document for QIP testing",
    )
    story = [
        Spacer(1, 7 * mm),
        Paragraph(machine["title"], styles["DocTitle"]),
        Paragraph(machine["kind"], styles["Subtitle"]),
        Table(
            [["ASSET REFERENCE", machine["code"]], ["DOCUMENT STATUS", machine["revision"]], ["DATA CLASS", "Entirely fictional / non-production"]],
            colWidths=[48 * mm, 112 * mm],
            style=TableStyle([
                ("BACKGROUND", (0, 0), (0, -1), INK),
                ("TEXTCOLOR", (0, 0), (0, -1), colors.white),
                ("BACKGROUND", (1, 0), (1, -1), PALE),
                ("TEXTCOLOR", (1, 0), (1, -1), INK),
                ("FONTNAME", (0, 0), (-1, -1), "Helvetica-Bold"),
                ("FONTSIZE", (0, 0), (-1, -1), 8),
                ("GRID", (0, 0), (-1, -1), 0.5, LINE),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("TOPPADDING", (0, 0), (-1, -1), 8),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
            ]),
        ),
        Spacer(1, 9 * mm),
        Paragraph("1. Purpose and equipment summary", styles["Section"]),
        Paragraph(machine["overview"], styles["BodyTextQip"]),
        Paragraph("This manual exists only to exercise document upload, PDF extraction, page provenance, retrieval, and citation behavior in QIP. It does not describe real equipment and must not be used for physical maintenance.", styles["Callout"]),
        Spacer(1, 3 * mm),
        Paragraph("Nominal operating envelope", styles["Subsection"]),
        Table(
            [["Parameter", "Synthetic value"]] + machine["specs"],
            colWidths=[78 * mm, 82 * mm],
            repeatRows=1,
            style=TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), INK),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("FONTNAME", (0, 1), (-1, -1), "Helvetica"),
                ("FONTSIZE", (0, 0), (-1, -1), 8.5),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PALE]),
                ("GRID", (0, 0), (-1, -1), 0.4, LINE),
                ("TOPPADDING", (0, 0), (-1, -1), 6),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
            ]),
        ),
        PageBreak(),
        Paragraph("2. Alarm and troubleshooting reference", styles["Section"]),
        Paragraph("Preserve observations before resetting an alarm. The table provides decision support for synthetic investigations; it does not establish root cause by itself.", styles["BodyTextQip"]),
    ]

    for code, trigger, response in machine["symptoms"]:
        story.append(KeepTogether([
            Table(
                [[code, trigger]],
                colWidths=[24 * mm, 136 * mm],
                style=TableStyle([
                    ("BACKGROUND", (0, 0), (0, 0), LIME),
                    ("BACKGROUND", (1, 0), (1, 0), PALE),
                    ("TEXTCOLOR", (0, 0), (-1, -1), INK),
                    ("FONTNAME", (0, 0), (-1, -1), "Helvetica-Bold"),
                    ("FONTSIZE", (0, 0), (-1, -1), 9),
                    ("BOX", (0, 0), (-1, -1), 0.6, LINE),
                    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                    ("TOPPADDING", (0, 0), (-1, -1), 7),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
                ]),
            ),
            Paragraph(f"Recommended inspection: {response}", styles["BodyTextQip"]),
            Spacer(1, 3 * mm),
        ]))

    story.extend([
        Spacer(1, 3 * mm),
        Table([[Paragraph("INVESTIGATION EXERCISE", styles["Small"]), Paragraph(machine["scenario"], styles["BodyTextQip"])]], colWidths=[42 * mm, 118 * mm], style=TableStyle([
            ("BACKGROUND", (0, 0), (0, 0), INK),
            ("TEXTCOLOR", (0, 0), (0, 0), colors.white),
            ("BACKGROUND", (1, 0), (1, 0), WARNING),
            ("BOX", (0, 0), (-1, -1), 0.6, LINE),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("TOPPADDING", (0, 0), (-1, -1), 9),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 9),
            ("LEFTPADDING", (0, 0), (-1, -1), 8),
            ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ])),
        PageBreak(),
        Paragraph("3. Inspection and maintenance", styles["Section"]),
        Paragraph("Use the following synthetic intervals to test evidence capture, question answering, and citation to a specific page.", styles["BodyTextQip"]),
    ])
    story.extend(bullet(item) for item in machine["checks"])
    story.extend([
        Spacer(1, 7 * mm),
        Paragraph("Safety boundary", styles["Section"]),
        Table([[Paragraph("TEST DATA ONLY", styles["Small"]), Paragraph("Lockout, isolation, and competent-person requirements in this fictional document are illustrative. Never apply these values or procedures to real machinery.", styles["BodyTextQip"])]], colWidths=[42 * mm, 118 * mm], style=TableStyle([
            ("BACKGROUND", (0, 0), (0, 0), INK),
            ("TEXTCOLOR", (0, 0), (0, 0), colors.white),
            ("BACKGROUND", (1, 0), (1, 0), colors.HexColor("#F6E3DF")),
            ("BOX", (0, 0), (-1, -1), 0.6, LINE),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("TOPPADDING", (0, 0), (-1, -1), 9),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 9),
            ("LEFTPADDING", (0, 0), (-1, -1), 8),
            ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ])),
        Spacer(1, 10 * mm),
        Paragraph("Suggested QIP test questions", styles["Section"]),
        bullet("What observation should be preserved before changing a machine setting?"),
        bullet("Which threshold in the source supports the recommended first inspection?"),
        bullet("Is there enough evidence to state a confirmed root cause? Explain the uncertainty."),
    ])
    document.build(story, onFirstPage=page_header_footer, onLaterPages=page_header_footer)
    return output_path


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for machine in MACHINES:
        path = make_pdf(machine)
        print(path)


if __name__ == "__main__":
    main()
