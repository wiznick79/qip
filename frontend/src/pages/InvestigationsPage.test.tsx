import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { InvestigationsPage } from "./InvestigationsPage";

const incident = {
  id: "incident-1", assetId: "asset-1", title: "Synthetic pump leak", description: "Oil near the seal",
  severity: "HIGH", status: "REPORTED", occurredAt: "2026-08-20T09:00:00Z",
  createdAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:00:00Z",
};
const document = {
  id: "document-1", title: "Synthetic pump manual", originalFilename: "pump.txt", mediaType: "text/plain",
  sizeBytes: 120, checksumSha256: "a".repeat(64), status: "INDEXED", failureReason: null,
  extractedPageCount: 1, uploadedAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:00:01Z",
};
const groundedQuestion = {
  id: "question-1", question: "What should be inspected?", selectedDocumentIds: ["document-1"],
  status: "GROUNDED", answer: "Inspect the synthetic hydraulic seal.", modelId: "deterministic-grounded-v1",
  promptVersion: "grounded-answer-v1", retrievedPassageCount: 1, failureReason: null,
  askedAt: "2026-08-20T10:01:00Z", completedAt: "2026-08-20T10:01:01Z",
  citations: [{ passageId: "passage-1", documentId: "document-1", documentTitle: "Synthetic pump manual",
    pageNumber: 1, passageSequence: 0, excerpt: "Inspect the synthetic hydraulic seal.", relevanceScore: 0.88 }],
};

describe("Investigation workspace", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      const url = String(input);
      if (url.startsWith("/api/incidents?")) return json({ items: [incident], page: 0, size: 100, totalElements: 1 });
      if (url.startsWith("/api/documents?")) return json({ items: [document], page: 0, size: 100, totalElements: 1 });
      if (url === "/api/incidents/incident-1/investigations") return json({ id: "investigation-1", incidentId: "incident-1", questions: [], createdAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:00:00Z" });
      if (url === "/api/investigations/investigation-1/questions" && init?.method === "POST") return json(groundedQuestion);
      if (url === "/api/investigations/investigation-1") return json({ id: "investigation-1", incidentId: "incident-1", questions: [groundedQuestion], createdAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:01:01Z" });
      return new Response(null, { status: 404 });
    });
  });

  afterEach(() => vi.restoreAllMocks());

  it("opens a case, asks against selected documents, and renders passage provenance", async () => {
    render(<InvestigationsPage />);
    fireEvent.change(await screen.findByLabelText("Incident"), { target: { value: "incident-1" } });
    fireEvent.click(screen.getByRole("button", { name: "Open investigation" }));
    expect(await screen.findByText("No questions yet")).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText(/Synthetic pump manual/i));
    fireEvent.change(screen.getByLabelText("Question"), { target: { value: "What should be inspected?" } });
    fireEvent.click(screen.getByRole("button", { name: "Ask with evidence" }));

    expect(await screen.findAllByText("Inspect the synthetic hydraulic seal.")).toHaveLength(2);
    expect(screen.getByText("GROUNDED")).toBeInTheDocument();
    fireEvent.click(screen.getByText(/Synthetic pump manual · page 1/));
    expect(screen.getByText("Passage 1 · relevance 0.880")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByLabelText("Question")).toHaveValue(""));
  });
});

function json(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { "Content-Type": "application/json" } });
}
